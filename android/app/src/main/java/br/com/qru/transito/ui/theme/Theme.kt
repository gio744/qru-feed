package br.com.qru.transito.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val QruColors=darkColorScheme(
    primary=Color(0xFF1677E8), secondary=Color(0xFF76B9FF),
    background=Color(0xFF09090B), surface=Color(0xFF141419),
    surfaceVariant=Color(0xFF1D1D24), onPrimary=Color.White,
    onBackground=Color(0xFFF3F7FF), onSurface=Color(0xFFF3F7FF)
)
@Composable fun QruTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme=QruColors,content=content)
