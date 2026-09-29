package com.example.realmebuds.theme

import androidx.compose.ui.graphics.Color

// Realme Link Clean UI Palette (Matched to official screenshot)
val RealmePageBg = Color(0xFFD5DADF)
val RealmeCardWhite = Color(0xFFFFFFFF)
val RealmeTextDark = Color(0xFF141619)
val RealmeTextGrey = Color(0xFF767C82)
val RealmeTextLight = Color(0xFF9EA3A8)

val RealmeCircleActive = Color(0xFF14171A)
val RealmeCircleInactive = Color(0xFFF0F2F5)
val RealmeIconActive = Color(0xFFFFFFFF)
val RealmeIconInactive = Color(0xFF6B7280)

val RealmeSwitchActive = Color(0xFF28C76F) // ColorOS vibrant green
val RealmeSwitchInactiveTrack = Color(0xFFE5E7EB)
val RealmeSwitchInactiveThumb = Color(0xFF9CA3AF)

val RealmeDivider = Color(0xFFF3F4F6)
val RealmePillBg = Color(0xFFE8ECEF)

val StatusSuccess = Color(0xFF10B981)
val StatusDanger = Color(0xFFEF4444)
val StatusWarning = Color(0xFFF59E0B)

// ------------------------------------------------------------------
// Depth & structure
// ------------------------------------------------------------------

/** Hairline outline for the white cards so they read as "soft objects" on the grey page. */
val RealmeCardBorder = Color(0xFFE4E8EC)

/** Tonal surface for leading icon tiles inside cards. */
val RealmeSurfaceSoft = Color(0xFFF4F6F8)

// ------------------------------------------------------------------
// Status pill tints — pre-computed once so no UI path allocates colours.
// ------------------------------------------------------------------
val StatusSuccessTint = StatusSuccess.copy(alpha = 0.13f)
val StatusWarningTint = StatusWarning.copy(alpha = 0.16f)
val StatusDangerTint = StatusDanger.copy(alpha = 0.12f)
val StatusDangerBorder = StatusDanger.copy(alpha = 0.28f)
val StatusNeutralTint = RealmeTextGrey.copy(alpha = 0.12f)

// ------------------------------------------------------------------
// Battery thresholds: >= 50 green, 20-49 amber, < 20 red, unknown grey.
// Text colours are darkened variants so 11-13sp text keeps AA contrast.
// ------------------------------------------------------------------
val BatteryLevelHigh = Color(0xFF15803D)
val BatteryLevelMedium = Color(0xFFB45309)
val BatteryLevelLow = Color(0xFFDC2626)
val BatteryLevelUnknown = Color(0xFF8A9099)

val BatteryLevelHighTint = BatteryLevelHigh.copy(alpha = 0.12f)
val BatteryLevelMediumTint = BatteryLevelMedium.copy(alpha = 0.13f)
val BatteryLevelLowTint = BatteryLevelLow.copy(alpha = 0.12f)
val BatteryLevelUnknownTint = BatteryLevelUnknown.copy(alpha = 0.13f)

// ------------------------------------------------------------------
// Hero (product render) treatment
// ------------------------------------------------------------------
val RealmeHeroGlowTop = Color(0xFFFFFFFF)
val RealmeHeroGlowBottom = Color(0xFFE8ECF1)
val RealmeHeroCircle = Color(0xFFF1F3F6)

// ------------------------------------------------------------------
// Protocol monitor terminal palette (hoisted so log rendering never allocates)
// ------------------------------------------------------------------
val TermBackground = Color(0xFF0F1013)
val TermTx = Color(0xFFFFC700)
val TermRx = Color(0xFF60A5FA)
val TermSuccess = Color(0xFF10B981)
val TermError = Color(0xFFEF4444)
val TermWarn = Color(0xFFF59E0B)
val TermInfo = Color(0xFFCCCCCC)
val TermMuted = Color(0xFF888888)
