package br.com.qru.transito.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// QRU NEO: midnight-blue surfaces and selective electric-blue accents.
// Kept on a feature branch until accessibility and device checks are complete.
private val QruColors=darkColorScheme(
    primary=Color(0xFF62B9FF),
    secondary=Color(0xFF9BD7FF),
    tertiary=Color(0xFF63D8BD),
    background=Color(0xFF080D17),
    surface=Color(0xFF101D30),
    surfaceVariant=Color(0xFF172B43),
    surfaceContainer=Color(0xFF101D30),
    surfaceContainerHigh=Color(0xFF172B43),
    onPrimary=Color(0xFF061525),
    onBackground=Color(0xFFF2F7FF),
    onSurface=Color(0xFFF2F7FF),
    onSurfaceVariant=Color(0xFFCFDEED),
    outline=Color(0xFF52718F)
)
@Composable fun QruTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme=QruColors,content=content)
