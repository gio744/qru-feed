package br.com.qru.transito.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val QruColors=darkColorScheme(
    primary=Color(0xFF9B5CFF), secondary=Color(0xFFC7A8FF),
    background=Color(0xFF09090B), surface=Color(0xFF141419),
    surfaceVariant=Color(0xFF1D1D24), onPrimary=Color.White,
    onBackground=Color(0xFFF5F2FA), onSurface=Color(0xFFF5F2FA)
)
@Composable fun QruTheme(content:@Composable()->Unit)=MaterialTheme(colorScheme=QruColors,content=content)
