package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/** Fütüristik, Türkçe karakterleri tam destekleyen yazı ailesi (Exo 2, OFL lisanslı). */
val Exo2 = FontFamily(
    Font(R.font.exo2_regular, FontWeight.Normal),
    Font(R.font.exo2_medium, FontWeight.Medium),
    Font(R.font.exo2_semibold, FontWeight.SemiBold),
    Font(R.font.exo2_bold, FontWeight.Bold),
    Font(R.font.exo2_extrabold, FontWeight.ExtraBold)
)

private fun style(weight: FontWeight, size: Int, line: Int, spacing: Double = 0.0) = TextStyle(
    fontFamily = Exo2,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.sp
)

val Typography = Typography(
    displayLarge = style(FontWeight.ExtraBold, 48, 54, -0.5),
    displayMedium = style(FontWeight.ExtraBold, 40, 46, -0.5),
    displaySmall = style(FontWeight.Bold, 34, 40),
    headlineLarge = style(FontWeight.ExtraBold, 30, 36, -0.3),
    headlineMedium = style(FontWeight.Bold, 24, 30),
    headlineSmall = style(FontWeight.Bold, 20, 26),
    titleLarge = style(FontWeight.Bold, 20, 26),
    titleMedium = style(FontWeight.SemiBold, 16, 22),
    titleSmall = style(FontWeight.SemiBold, 14, 20),
    bodyLarge = style(FontWeight.Normal, 16, 23),
    bodyMedium = style(FontWeight.Normal, 14, 20),
    bodySmall = style(FontWeight.Normal, 12, 17),
    labelLarge = style(FontWeight.SemiBold, 14, 18, 0.2),
    labelMedium = style(FontWeight.SemiBold, 12, 16, 0.3),
    labelSmall = style(FontWeight.Medium, 11, 14, 0.4)
)
