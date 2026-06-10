package com.alecdev.quickcalc.presentation.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Typography
import com.alecdev.quickcalc.R

@OptIn(ExperimentalTextApi::class)
val RoundedFontFamily = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("ROND", 100f),
            FontVariation.Setting("opsz", 8f)
        )
    )
)

val Typography = Typography(
    body1 = TextStyle(
        fontFamily = RoundedFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    button = TextStyle(
        fontFamily = RoundedFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    display3 = TextStyle(
        fontFamily = RoundedFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp
    )
)