package br.com.qru.transito.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// QRU dark navy / graphite with restrained metallic-blue accents.
private val QruColors=darkColorScheme(
    primary=Color(0xFF69A8E8),
    secondary=Color(0xFFC1D4E6),
    tertiary=Color(0xFF91B5D5),
    background=Color(0xFF070C15),
    surface=Color(0xFF101A2A),
    surfaceVariant=Color(0xFF1B2A3D),
    surfaceContainer=Color(0xFF101A2A),
    surfaceContainerHigh=Color(0xFF1B2A3D),
    onPrimary=Color(0xFF071423),
    onBackground=Color(0xFFEAF0F7),
    onSurface=Color(0xFFEAF0F7),
    onSurfaceVariant=Color(0xFFB8C7D8),
    outline=Color(0xFF52667D)
)
@Composable fun QruTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme=QruColors,content=content)
