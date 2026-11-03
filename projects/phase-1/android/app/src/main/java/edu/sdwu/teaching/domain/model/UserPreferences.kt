package edu.sdwu.teaching.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Material You colors from the wallpaper (Android 12+ only). */
    val dynamicColor: Boolean = false,
)
