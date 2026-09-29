package com.example.realmebuds.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Deliberate type scale for the companion app.
 *
 * Everything in the UI should reference one of these styles instead of hard-coded
 * `sp` values so the hierarchy stays consistent (and scales cleanly with the
 * system font-size setting, since every size is expressed in `sp`).
 */
object RealmeType {
    /** App-bar / hero headline. */
    val ScreenTitle = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )

    /** Live status pill under the app-bar title. */
    val StatusPill = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp
    )

    /** Card section heading ("Noise control", "Other", …). */
    val SectionTitle = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )

    /** Row title inside a card ("Game mode", …). */
    val RowTitle = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.1.sp
    )

    /** Dialog titles. */
    val DialogTitle = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    )

    /** Default reading text. */
    val BodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )

    val Body = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    )

    /** Supporting copy, captions, metadata. */
    val Caption = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.1.sp
    )

    /** Chips, pill labels, small button labels. */
    val Chip = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp
    )

    /** Battery percentage / compact numeric readouts. */
    val MetricValue = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )

    /** Primary CTA label. */
    val ButtonLabel = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.3.sp
    )

    /** Protocol monitor terminal lines. */
    val Terminal = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
}

// Set of Material typography styles, mapped onto the app type scale so that any
// Material3 component (dialogs, switches, text fields…) inherits the same look.
val Typography =
    Typography(
        headlineSmall = RealmeType.ScreenTitle,
        titleLarge = RealmeType.DialogTitle,
        titleMedium = RealmeType.SectionTitle,
        titleSmall = RealmeType.RowTitle,
        bodyLarge = RealmeType.BodyLarge,
        bodyMedium = RealmeType.Body,
        bodySmall = RealmeType.Caption,
        labelLarge = RealmeType.ButtonLabel,
        labelMedium = RealmeType.StatusPill,
        labelSmall = RealmeType.Chip
    )
