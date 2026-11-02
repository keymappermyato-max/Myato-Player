package com.example.model

enum class ThemePreset(
    val id: String,
    val title: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val backgroundHex: Long,
    val surfaceHex: Long
) {
    BLACK_PURPLE("black_purple", "Preto e Roxo (Neon)", 0xFFA855F7, 0xFF38BDF8, 0xFF0D0B14, 0xFF171324),
    BLACK_BLUE("black_blue", "Preto e Azul (Elétrico)", 0xFF38BDF8, 0xFF818CF8, 0xFF080E1A, 0xFF101B2E),
    BLACK_RED("black_red", "Preto e Vermelho (Carmim)", 0xFFEF4444, 0xFFF97316, 0xFF140808, 0xFF241010),
    BLACK_GREEN("black_green", "Preto e Verde (Matrix)", 0xFF22C55E, 0xFF10B981, 0xFF08120B, 0xFF0E2214),
    BLACK_ORANGE("black_orange", "Preto e Laranja (Sunset)", 0xFFF97316, 0xFFFBBF24, 0xFF140D07, 0xFF24170E),
    BLACK_PINK("black_pink", "Preto e Rosa (Cyber)", 0xFFEC4899, 0xFFA855F7, 0xFF140812, 0xFF241021),
    MIDNIGHT_BLUE("midnight_blue", "Azul Noturno", 0xFF6366F1, 0xFF38BDF8, 0xFF0F172A, 0xFF1E293B),
    EMERALD_GREEN("emerald_green", "Verde Esmeralda", 0xFF10B981, 0xFF06B6D4, 0xFF041A14, 0xFF092E24),
    MINIMAL_WHITE("minimal_white", "Branco Minimalista", 0xFF7C3AED, 0xFF2563EB, 0xFFF8FAFC, 0xFFFFFFFF),
    CUSTOM("custom", "Tema Personalizado", 0xFFA855F7, 0xFF38BDF8, 0xFF0D0B14, 0xFF171324)
}

data class AppSettings(
    val themePreset: ThemePreset = ThemePreset.BLACK_PURPLE,
    val customPrimaryColor: Long = 0xFFA855F7,
    val customSecondaryColor: Long = 0xFF38BDF8,
    val glowIntensity: Float = 0.8f,
    val enableAnimations: Boolean = true,
    val backgroundUri: String? = null,
    val backgroundBlur: Float = 16f,
    val backgroundDimAlpha: Float = 0.65f,
    val playbackSpeed: Float = 1.0f,
    val gaplessPlayback: Boolean = true,
    val rememberPlaybackPosition: Boolean = true,
    val visualizerBarsCount: Int = 24
)
