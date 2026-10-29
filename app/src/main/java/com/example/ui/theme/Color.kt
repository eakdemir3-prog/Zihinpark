package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// ZİHİNPARK PALETLERİ: Neon (koyu) ve Açık (lavanta)
// Renkler anlık durum (snapshot state) üzerinden okunur; tema değişince
// tüm ekranlar ve çizimler kendiliğinden yenilenir.
// ==========================================

class ZpPalette(
    val isLight: Boolean,
    val bg: Color, val bgDeep: Color, val surface: Color, val card: Color, val cardHigh: Color, val border: Color,
    val cyan: Color, val pink: Color, val purple: Color, val green: Color, val yellow: Color, val orange: Color, val red: Color, val blue: Color,
    val cyanDim: Color, val pinkDim: Color, val purpleDim: Color, val greenDim: Color,
    val textPrimary: Color, val textSecondary: Color, val textMuted: Color, val onNeon: Color,
    val lavender: Color, val lavenderCard: Color, val yellowTint: Color, val peach: Color, val blueTint: Color
)

val NeonPalette = ZpPalette(
    isLight = false,
    bg = Color(0xFF0A0A14), bgDeep = Color(0xFF06060D), surface = Color(0xFF12121F), card = Color(0xFF1A1A2E), cardHigh = Color(0xFF23233D), border = Color(0xFF2C2C4A),
    cyan = Color(0xFF00F0FF), pink = Color(0xFFFF2E97), purple = Color(0xFFB026FF), green = Color(0xFF39FF14), yellow = Color(0xFFFFE600), orange = Color(0xFFFF8A00), red = Color(0xFFFF3B5C), blue = Color(0xFF3D7BFF),
    cyanDim = Color(0xFF0E2E36), pinkDim = Color(0xFF33102A), purpleDim = Color(0xFF26103A), greenDim = Color(0xFF0E2A18),
    textPrimary = Color(0xFFF2F4FF), textSecondary = Color(0xFF9AA0C3), textMuted = Color(0xFF62678A), onNeon = Color(0xFF07070F),
    lavender = Color(0xFF22193A), lavenderCard = Color(0xFF1E1A30), yellowTint = Color(0xFF2A2410), peach = Color(0xFF2E1622), blueTint = Color(0xFF0F1C36)
)

/** Açık tema: uygulama simgesindeki lavanta zemin + turkuaz/pembe vurgular. */
val LightPalette = ZpPalette(
    isLight = true,
    bg = Color(0xFFF4EFFF), bgDeep = Color(0xFFEAE3FF), surface = Color(0xFFFBF9FF), card = Color(0xFFFFFFFF), cardHigh = Color(0xFFEFE9FF), border = Color(0xFFDCD2F7),
    cyan = Color(0xFF00A8C8), pink = Color(0xFFE6227E), purple = Color(0xFF8A2BE2), green = Color(0xFF14A44D), yellow = Color(0xFFD99A00), orange = Color(0xFFEE7200), red = Color(0xFFE0304E), blue = Color(0xFF2F62E0),
    cyanDim = Color(0xFFDDF6FB), pinkDim = Color(0xFFFCE1EE), purpleDim = Color(0xFFEDE0FC), greenDim = Color(0xFFDDF5E6),
    textPrimary = Color(0xFF1E1440), textSecondary = Color(0xFF5B5480), textMuted = Color(0xFF9A93B8), onNeon = Color(0xFFFFFFFF),
    lavender = Color(0xFFEDE6FF), lavenderCard = Color(0xFFF3EEFF), yellowTint = Color(0xFFFFF6D6), peach = Color(0xFFFFE8EE), blueTint = Color(0xFFE3ECFF)
)

object ZpColors {
    var palette by mutableStateOf(NeonPalette)
}
private val P get() = ZpColors.palette

// Zemin katmanları (derinden yükseğe)
val NeonBg get() = P.bg
val NeonBgDeep get() = P.bgDeep
val NeonSurface get() = P.surface
val NeonCard get() = P.card
val NeonCardHigh get() = P.cardHigh
val NeonBorder get() = P.border

// Vurgu renkleri
val NeonCyan get() = P.cyan
val NeonPink get() = P.pink
val NeonPurple get() = P.purple
val NeonGreen get() = P.green
val NeonYellow get() = P.yellow
val NeonOrange get() = P.orange
val NeonRed get() = P.red
val NeonBlue get() = P.blue
val NeonCyanDim get() = P.cyanDim
val NeonPinkDim get() = P.pinkDim
val NeonPurpleDim get() = P.purpleDim
val NeonGreenDim get() = P.greenDim

// Metin
val TextPrimary get() = P.textPrimary
val TextSecondary get() = P.textSecondary
val TextMuted get() = P.textMuted
val OnNeon get() = P.onNeon // Vurgu rengi zemin üstündeki yazı

// Hazır geçişler
val NeonCyanPurple get() = Brush.linearGradient(listOf(NeonCyan, NeonPurple))
val NeonPinkPurple get() = Brush.linearGradient(listOf(NeonPink, NeonPurple))
val NeonGlassBrush get() = Brush.verticalGradient(
    if (P.isLight) listOf(Color(0x14FFFFFF), Color(0x0A8A2BE2)) else listOf(Color(0x332C2C4A), Color(0x1A12121F))
)

// ==========================================
// ESKİ İSİMLER (mevcut ekranlar bozulmasın diye)
// ==========================================
val WarmCreamBackground get() = NeonBg
val SoftCreamSurface get() = NeonSurface
val PrimaryCoral get() = NeonCyan
val PrimaryCoralVariant get() = NeonPink
val AccentSalmon get() = NeonOrange

val PastelLavender get() = P.lavender
val LavenderCard get() = P.lavenderCard
val PastelYellow get() = P.yellowTint
val MintGreen get() = P.greenDim
val MintGreenDark get() = NeonGreen
val PastelPeach get() = P.peach
val PastelBlue get() = P.blueTint
val PastelBlueDark get() = NeonBlue

val DarkSlateText get() = TextPrimary
val MediumGrayText get() = TextSecondary
val LightGrayBorder get() = NeonBorder

val DarkBackground get() = NeonBg
val DarkSurface get() = NeonSurface
val DarkCardBg get() = NeonCard
val DarkCoralPrimary get() = NeonCyan
val DarkTextPrimary get() = TextPrimary
val DarkTextSecondary get() = TextSecondary
val DarkBorder get() = NeonBorder
