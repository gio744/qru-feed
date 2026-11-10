package br.com.qru.transito.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Identidade QRU TRÂNSITO: azul e grafite, sem acentos roxos.
private val QruColors = darkColorScheme(
    primary = Color(0xFF258BFF),
    secondary = Color(0xFF79BCFF),
    background = Color(0xFF080E19),
    surface = Color(0xFF111B2A),
    surfaceVariant = Color(0xFF1A2B40),
    onPrimary = Color.White,
    onBackground = Color(0xFFF3F7FF),
    onSurface = Color(0xFFF3F7FF)
)

@Composable
fun QruTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = QruColors, content = content)
