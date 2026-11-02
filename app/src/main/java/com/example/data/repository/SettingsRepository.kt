package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AppSettings
import com.example.model.ThemePreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("myato_settings_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val themeId = prefs.getString("theme_preset", ThemePreset.BLACK_PURPLE.id)
            ?: ThemePreset.BLACK_PURPLE.id
        val preset = ThemePreset.entries.find { it.id == themeId } ?: ThemePreset.BLACK_PURPLE

        return AppSettings(
            themePreset = preset,
            customPrimaryColor = prefs.getLong("custom_primary_color", 0xFFA855F7),
            customSecondaryColor = prefs.getLong("custom_secondary_color", 0xFF38BDF8),
            glowIntensity = prefs.getFloat("glow_intensity", 0.8f),
            enableAnimations = prefs.getBoolean("enable_animations", true),
            backgroundUri = prefs.getString("background_uri", null),
            backgroundBlur = prefs.getFloat("background_blur", 16f),
            backgroundDimAlpha = prefs.getFloat("background_dim_alpha", 0.65f),
            playbackSpeed = prefs.getFloat("playback_speed", 1.0f),
            gaplessPlayback = prefs.getBoolean("gapless_playback", true),
            rememberPlaybackPosition = prefs.getBoolean("remember_playback_position", true),
            visualizerBarsCount = prefs.getInt("visualizer_bars", 24)
        )
    }

    fun updateTheme(preset: ThemePreset) {
        prefs.edit().putString("theme_preset", preset.id).apply()
        _settings.value = _settings.value.copy(themePreset = preset)
    }

    fun updateCustomColors(primaryHex: Long, secondaryHex: Long) {
        prefs.edit()
            .putLong("custom_primary_color", primaryHex)
            .putLong("custom_secondary_color", secondaryHex)
            .putString("theme_preset", ThemePreset.CUSTOM.id)
            .apply()
        _settings.value = _settings.value.copy(
            themePreset = ThemePreset.CUSTOM,
            customPrimaryColor = primaryHex,
            customSecondaryColor = secondaryHex
        )
    }

    fun updateGlowIntensity(intensity: Float) {
        prefs.edit().putFloat("glow_intensity", intensity).apply()
        _settings.value = _settings.value.copy(glowIntensity = intensity)
    }

    fun updateEnableAnimations(enabled: Boolean) {
        prefs.edit().putBoolean("enable_animations", enabled).apply()
        _settings.value = _settings.value.copy(enableAnimations = enabled)
    }

    fun updateBackground(uriString: String?, blur: Float = 16f, dimAlpha: Float = 0.65f) {
        prefs.edit()
            .putString("background_uri", uriString)
            .putFloat("background_blur", blur)
            .putFloat("background_dim_alpha", dimAlpha)
            .apply()
        _settings.value = _settings.value.copy(
            backgroundUri = uriString,
            backgroundBlur = blur,
            backgroundDimAlpha = dimAlpha
        )
    }

    fun updatePlaybackSpeed(speed: Float) {
        prefs.edit().putFloat("playback_speed", speed).apply()
        _settings.value = _settings.value.copy(playbackSpeed = speed)
    }

    fun updateGaplessPlayback(enabled: Boolean) {
        prefs.edit().putBoolean("gapless_playback", enabled).apply()
        _settings.value = _settings.value.copy(gaplessPlayback = enabled)
    }

    fun updateRememberPosition(enabled: Boolean) {
        prefs.edit().putBoolean("remember_playback_position", enabled).apply()
        _settings.value = _settings.value.copy(rememberPlaybackPosition = enabled)
    }

    // Equalizer persistence
    fun isEqualizerEnabled(): Boolean = prefs.getBoolean("eq_enabled", true)
    fun setEqualizerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("eq_enabled", enabled).apply()
    }

    fun getEqualizerPreset(): String = prefs.getString("eq_preset_name", "Normal") ?: "Normal"
    fun setEqualizerPreset(name: String) {
        prefs.edit().putString("eq_preset_name", name).apply()
    }

    fun getBassBoostStrength(): Int = prefs.getInt("eq_bass_boost", 400)
    fun setBassBoostStrength(strength: Int) {
        prefs.edit().putInt("eq_bass_boost", strength).apply()
    }

    fun getVirtualizerStrength(): Int = prefs.getInt("eq_virtualizer", 300)
    fun setVirtualizerStrength(strength: Int) {
        prefs.edit().putInt("eq_virtualizer", strength).apply()
    }

    fun getBandLevel(band: Int, defaultLevel: Short): Short {
        return prefs.getInt("eq_band_$band", defaultLevel.toInt()).toShort()
    }

    fun setBandLevel(band: Int, level: Short) {
        prefs.edit().putInt("eq_band_$band", level.toInt()).apply()
    }

    // Queue persistence
    fun saveLastQueue(songIds: List<Long>, currentIndex: Int, positionMs: Long) {
        prefs.edit()
            .putString("last_queue_ids", songIds.joinToString(","))
            .putInt("last_queue_index", currentIndex)
            .putLong("last_position_ms", positionMs)
            .apply()
    }

    fun getLastQueueSongIds(): List<Long> {
        val raw = prefs.getString("last_queue_ids", null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    fun getLastQueueIndex(): Int = prefs.getInt("last_queue_index", 0)

    fun getLastPositionMs(): Long = prefs.getLong("last_position_ms", 0L)

    fun restoreDefaults() {
        prefs.edit().clear().apply()
        _settings.value = loadSettings()
    }
}
