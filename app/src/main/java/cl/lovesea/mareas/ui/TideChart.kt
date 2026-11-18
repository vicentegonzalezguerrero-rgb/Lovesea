package cl.lovesea.mareas.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.lovesea.mareas.verdict.Level
import java.time.Instant

@Composable
fun TideChart(curve: List<Pair<Instant, Double>>, now: Instant?) {
    val t0 = curve.first().first.epochSecond.toFloat()
    val t1 = curve.last().first.epochSecond.toFloat()
    val maxH = maxOf(1.6, curve.maxOf { it.second }).toFloat()
    val marker = Level.ORANGE.color()

    Column {
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            fun x(sec: Long) = (sec - t0) / (t1 - t0) * size.width
            fun y(h: Double) = size.height - (h.toFloat() / maxH) * size.height

            for (i in 1..3) {
                val gx = size.width * i / 4
                drawLine(Line, Offset(gx, 0f), Offset(gx, size.height),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
            }
            val path = Path()
            curve.forEachIndexed { i, (t, h) ->
                if (i == 0) path.moveTo(x(t.epochSecond), y(h)) else path.lineTo(x(t.epochSecond), y(h))
            }
            val area = Path().apply {
                addPath(path)
                lineTo(x(curve.last().first.epochSecond), size.height)
                lineTo(x(curve.first().first.epochSecond), size.height)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(Teal.copy(alpha = 0.45f), Teal.copy(alpha = 0.05f))))
            drawPath(path, Navy, style = Stroke(width = 3.dp.toPx()))
            if (now != null && now.epochSecond.toFloat() in t0..t1) {
                val nx = x(now.epochSecond)
                drawLine(marker, Offset(nx, 0f), Offset(nx, size.height), strokeWidth = 2.dp.toPx())
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("00", "06", "12", "18", "24").forEach { Text(it, fontSize = 11.sp, color = Muted) }
        }
    }
}
