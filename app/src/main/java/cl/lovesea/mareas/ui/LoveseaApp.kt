package cl.lovesea.mareas.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.os.Build
import cl.lovesea.mareas.data.Beaches
import cl.lovesea.mareas.tide.TideTable

@Composable
fun LoveseaApp(table: TideTable) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme(primary = Color(0xFF8ACEFF))
        else -> lightColorScheme(primary = Color(0xFF0B5C8A))
    }
    MaterialTheme(colorScheme = colors) {
        var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
        val beach = selectedId?.let { Beaches.byId(it) }
        if (beach == null) {
            BeachListScreen(table, onOpen = { selectedId = it.id })
        } else {
            BackHandler { selectedId = null }
            BeachDetailScreen(beach, table.shifted(beach.offsetMinutes * 60L), onBack = { selectedId = null })
        }
    }
}
