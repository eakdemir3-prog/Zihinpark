package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// ZİHİNPARK NEON PALETİ
// ==========================================

// Zemin katmanları (derinden yükseğe)
val NeonBg = Color(0xFF0A0A14)
val NeonBgDeep = Color(0xFF06060D)
val NeonSurface = Color(0xFF12121F)
val NeonCard = Color(0xFF1A1A2E)
val NeonCardHigh = Color(0xFF23233D)
val NeonBorder = Color(0xFF2C2C4A)

// Neon ışıklar
val NeonCyan = Color(0xFF00F0FF)
val NeonPink = Color(0xFFFF2E97)
val NeonPurple = Color(0xFFB026FF)
val NeonGreen = Color(0xFF39FF14)
val NeonYellow = Color(0xFFFFE600)
val NeonOrange = Color(0xFFFF8A00)
val NeonRed = Color(0xFFFF3B5C)
val NeonBlue = Color(0xFF3D7BFF)
val NeonCyanDim = Color(0xFF0E2E36)
val NeonPinkDim = Color(0xFF33102A)
val NeonPurpleDim = Color(0xFF26103A)
val NeonGreenDim = Color(0xFF0E2A18)

// Metin
val TextPrimary = Color(0xFFF2F4FF)
val TextSecondary = Color(0xFF9AA0C3)
val TextMuted = Color(0xFF62678A)
val OnNeon = Color(0xFF07070F) // Parlak neon zemin üstündeki koyu yazı

// Hazır geçişler
val NeonCyanPurple = Brush.linearGradient(listOf(NeonCyan, NeonPurple))
val NeonPinkPurple = Brush.linearGradient(listOf(NeonPink, NeonPurple))
val NeonGlassBrush = Brush.verticalGradient(
    listOf(Color(0x332C2C4A), Color(0x1A12121F))
)

// ==========================================
// ESKİ İSİMLER (mevcut ekranlar bozulmasın diye neon karşılıklarına bağlandı)
// ==========================================
val WarmCreamBackground = NeonBg
val SoftCreamSurface = NeonSurface
val PrimaryCoral = NeonCyan
val PrimaryCoralVariant = NeonPink
val AccentSalmon = NeonOrange

// Pastel zeminler -> koyu, renk tonlu cam zeminler
val PastelLavender = Color(0xFF22193A)
val LavenderCard = Color(0xFF1E1A30)
val PastelYellow = Color(0xFF2A2410)
val MintGreen = Color(0xFF0E2A18)
val MintGreenDark = NeonGreen
val PastelPeach = Color(0xFF2E1622)
val PastelBlue = Color(0xFF0F1C36)
val PastelBlueDark = NeonBlue

val DarkSlateText = TextPrimary
val MediumGrayText = TextSecondary
val LightGrayBorder = NeonBorder

val DarkBackground = NeonBg
val DarkSurface = NeonSurface
val DarkCardBg = NeonCard
val DarkCoralPrimary = NeonCyan
val DarkTextPrimary = TextPrimary
val DarkTextSecondary = TextSecondary
val DarkBorder = NeonBorder
