package cl.lovesea.mareas.tide

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Tabla de mareas: se descarga desde el repositorio para que se pueda corregir
 * sin publicar otra versión de la app. Si no hay internet usa la última descargada
 * o la que viene dentro de la app.
 */
class TideRepository(private val context: Context) {
    private val file = File(context.filesDir, "valparaiso.csv")

    fun local(): TideTable {
        val downloaded = runCatching { file.inputStream().use { TideTable.parse(it) } }.getOrNull()
        val bundled = runCatching { context.assets.open(ASSET).use { TideTable.parse(it) } }.getOrNull()
        return listOfNotNull(downloaded, bundled)
            .maxByOrNull { it.lastInstant?.epochSecond ?: 0 }
            ?: TideTable(emptyList())
    }

    /** Bloqueante. Devuelve la tabla nueva si se pudo descargar y es válida. */
    fun refresh(): TideTable? = runCatching {
        val c = URL(URL_CSV).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 20000
        try {
            if (c.responseCode != 200) return null
            val text = c.inputStream.bufferedReader().readText()
            val table = TideTable.parse(text.byteInputStream())
            if (table.events.size < 1000) return null
            file.writeText(text)
            table
        } finally {
            c.disconnect()
        }
    }.getOrNull()

    companion object {
        private const val ASSET = "tides/valparaiso.csv"
        private const val URL_CSV =
            "https://raw.githubusercontent.com/vicentegonzalezguerrero-rgb/Lovesea/main/app/src/main/assets/tides/valparaiso.csv"
    }
}
