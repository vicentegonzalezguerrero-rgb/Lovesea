package cl.lovesea.mareas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cl.lovesea.mareas.tide.TideTable
import cl.lovesea.mareas.ui.LoveseaApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Si la tabla falta o está dañada, la app abre igual y muestra "sin datos".
        val table = runCatching { assets.open("tides/valparaiso.csv").use { TideTable.parse(it) } }
            .getOrElse { TideTable(emptyList()) }
        setContent { LoveseaApp(table) }
    }
}
