package br.com.qru.transito.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Identidade QRU aprovada: preto profundo, prata e azul-neon.
private val QruColors=darkColorScheme(
    primary=Color(0xFF27B8FF),
    secondary=Color(0xFFA6D8FF),
    tertiary=Color(0xFFE2EAF4),
    background=Color(0xFF030A13),
    surface=Color(0xFF081625),
    surfaceVariant=Color(0xFF10263B),
    onPrimary=Color(0xFF001421),
    onBackground=Color(0xFFF1F6FC),
    onSurface=Color(0xFFF1F6FC),
    outline=Color(0xFF3389B9)
)
@Composable fun QruTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme=QruColors,content=content)
