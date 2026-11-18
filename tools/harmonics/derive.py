"""Genera la tabla de pleamares y bajamares para la app.

1. Descarga el nivel del mar horario observado en Valparaíso (UHSLC, estación 080).
2. Calcula las constantes armónicas con UTide.
3. Predice la marea minuto a minuto y extrae pleamares y bajamares.
4. Escribe app/src/main/assets/tides/valparaiso.csv

Uso: python tools/harmonics/derive.py --start 2025 --end 2036
"""
import argparse
import io
import json
import sys
import urllib.request
from pathlib import Path

import numpy as np
import pandas as pd
import utide

STATION = "080"
URLS = [
    f"https://uhslc.soest.hawaii.edu/data/csv/rqds/pacific/hourly/h{STATION}a.csv",
    f"https://uhslc.soest.hawaii.edu/data/csv/fast/hourly/h{STATION}.csv",
]
META = f"https://uhslc.soest.hawaii.edu/opendap/rqds/pacific/hourly/h{STATION}a.nc.das"
LAT = -33.03


def fetch(url):
    print("GET", url, file=sys.stderr)
    with urllib.request.urlopen(url, timeout=120) as r:
        return r.read().decode()


def load():
    frames = []
    for url in URLS:
        try:
            txt = fetch(url)
        except Exception as e:  # noqa: BLE001
            print("  falló:", e, file=sys.stderr)
            continue
        df = pd.read_csv(io.StringIO(txt), header=None, names=["y", "m", "d", "h", "v"])
        df["t"] = pd.to_datetime(dict(year=df.y, month=df.m, day=df.d, hour=df.h), utc=True)
        df = df[df.v > -32000]
        frames.append(df[["t", "v"]])
        print(f"  {len(df)} filas {df.t.min()} → {df.t.max()}", file=sys.stderr)
    if not frames:
        sys.exit("sin datos")
    df = pd.concat(frames).drop_duplicates("t").sort_values("t")
    return df


def extremes(times, h):
    d = np.diff(h)
    idx = np.where(np.sign(d[:-1]) != np.sign(d[1:]))[0] + 1
    out = []
    for i in idx:
        # refinamiento parabólico
        y0, y1, y2 = h[i - 1], h[i], h[i + 1]
        den = y0 - 2 * y1 + y2
        off = 0.5 * (y0 - y2) / den if den != 0 else 0.0
        hh = y1 - 0.25 * (y0 - y2) * off
        t = times[i] + pd.Timedelta(minutes=float(off))
        out.append((t, hh, "H" if den < 0 else "L"))
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--start", type=int, default=2025)
    ap.add_argument("--end", type=int, default=2036)
    ap.add_argument("--years-fit", type=int, default=8)
    ap.add_argument("--out", default="app/src/main/assets/tides/valparaiso.csv")
    a = ap.parse_args()

    try:
        das = fetch(META)
        for line in das.splitlines():
            if any(k in line.lower() for k in ("station", "name", "valpar", "latitude", "longitude")):
                print("META:", line.strip())
    except Exception as e:  # noqa: BLE001
        print("sin metadatos:", e)

    df = load()
    df = df[df.t >= df.t.max() - pd.Timedelta(days=365 * a.years_fit)]
    v = df.v.values / 1000.0
    print(f"Ajuste con {len(df)} horas, {df.t.min()} → {df.t.max()}")
    coef = utide.solve(df.t.dt.tz_localize(None).values, v, lat=LAT, method="ols",
                       conf_int="none", verbose=False)

    consts = [
        {"name": n, "amp": round(float(A), 4), "phase": round(float(g), 2)}
        for n, A, g in zip(coef.name, coef.A, coef.g)
        if A >= 0.002
    ]
    print("Constantes principales:")
    for c in consts[:12]:
        print(f"  {c['name']:6s} A={c['amp']:.3f} m  g={c['phase']:.1f}°")

    # Nivel de reducción aproximado: mínimo de 19 años de predicción horaria.
    tl = pd.date_range("2020-01-01", "2039-01-01", freq="1h")
    lat_level = utide.reconstruct(tl.values, coef, verbose=False).h.min()
    print(f"Nivel mínimo astronómico (respecto al cero de la estación): {lat_level:.3f} m")

    # Predicción minuto a minuto por meses para no agotar la memoria.
    ex = []
    months = pd.date_range(f"{a.start}-01-01", f"{a.end}-01-01", freq="MS")
    for m0, m1 in zip(months[:-1], months[1:]):
        t = pd.date_range(m0 - pd.Timedelta(minutes=2), m1 + pd.Timedelta(minutes=2), freq="1min")
        h = utide.reconstruct(t.values, coef, verbose=False).h - lat_level
        ex += [e for e in extremes(t.tz_localize("UTC"), h)
               if m0.tz_localize("UTC") <= e[0] < m1.tz_localize("UTC")]
    out = Path(a.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    with out.open("w") as f:
        f.write("# Pleamares (H) y bajamares (L) Valparaíso. Hora UTC epoch en segundos, altura en m sobre el nivel mínimo astronómico.\n")
        for tt, hh, k in ex:
            f.write(f"{int(tt.timestamp())},{hh:.2f},{k}\n")
    print(f"{len(ex)} extremos escritos en {out}")
    Path(out.with_suffix(".json")).write_text(json.dumps(
        {"station": "Valparaíso (UHSLC 080)", "fit_from": str(df.t.min()), "fit_to": str(df.t.max()),
         "datum_offset_m": round(float(lat_level), 3), "constituents": consts}, indent=1))

    # muestra para validar contra la tabla SHOA
    sample = [e for e in ex if pd.Timestamp("2026-11-18", tz="UTC") <= e[0] < pd.Timestamp("2026-11-22", tz="UTC")]
    print("Muestra (hora de Chile, UTC-3):")
    for tt, hh, k in sample:
        print(f"  {(tt - pd.Timedelta(hours=3)).strftime('%Y-%m-%d %H:%M')}  {hh:.2f} m  {k}")


if __name__ == "__main__":
    main()
