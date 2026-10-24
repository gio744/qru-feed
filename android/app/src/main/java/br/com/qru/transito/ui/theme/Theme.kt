package br.com.qru.transito.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// QRU premium dark: readable graphite surfaces with selective violet/magenta accents.
private val QruColors=darkColorScheme(
    primary=Color(0xFFA855F7),
    secondary=Color(0xFFD2A8FF),
    tertiary=Color(0xFFE63CCB),
    background=Color(0xFF09090F),
    surface=Color(0xFF191326),
    surfaceVariant=Color(0xFF251B33),
    surfaceContainer=Color(0xFF191326),
    surfaceContainerHigh=Color(0xFF251B33),
    onPrimary=Color(0xFF100719),
    onBackground=Color(0xFFF5F2FA),
    onSurface=Color(0xFFF5F2FA),
    onSurfaceVariant=Color(0xFFDDD1E9),
    outline=Color(0xFF78678D)
)
@Composable fun QruTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme=QruColors,content=content)
