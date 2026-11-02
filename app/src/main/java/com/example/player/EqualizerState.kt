package com.example.player

data class EqualizerBand(
    val bandIndex: Short,
    val centerFreqHz: Int,
    val minLevelMdB: Short,
    val maxLevelMdB: Short,
    val currentLevelMdB: Short
) {
    val displayFrequency: String
        get() = if (centerFreqHz >= 1000) {
            "${centerFreqHz / 1000} kHz"
        } else {
            "$centerFreqHz Hz"
        }

    val displayDb: String
        get() = "%+.1f dB".format(currentLevelMdB / 100.0)
}

data class EqualizerState(
    val isEnabled: Boolean = true,
    val currentPreset: String = "Normal",
    val presets: List<String> = listOf("Normal", "Pop", "Rock", "Jazz", "Clássico", "Eletrônica", "Hip-Hop", "Acústico", "Grave Reforçado", "Personalizado"),
    val bands: List<EqualizerBand> = emptyList(),
    val bassBoost: Int = 400, // 0 - 1000
    val virtualizer: Int = 300, // 0 - 1000
    val isBassBoostSupported: Boolean = true,
    val isVirtualizerSupported: Boolean = true
)
