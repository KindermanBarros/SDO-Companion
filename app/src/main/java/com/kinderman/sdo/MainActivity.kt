package com.kinderman.sdo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kinderman.sdo.presentation.SdoApp
import com.kinderman.sdo.ui.SdoContentDensity
import com.kinderman.sdo.ui.SdoFontScale
import com.kinderman.sdo.ui.SdoPreferences
import com.kinderman.sdo.ui.SdoTheme
import com.kinderman.sdo.ui.SdoThemeVariant
import com.kinderman.sdo.ui.SdoResponsiveFrame

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var preferences by remember { mutableStateOf(loadPreferences()) }
            SdoTheme(preferences) {
                SdoResponsiveFrame { SdoApp(
                    activity = this,
                    preferences = preferences,
                    onPreferencesChange = { updated ->
                        preferences = updated
                        savePreferences(updated)
                    },
                ) }
            }
        }
    }

    private fun loadPreferences(): SdoPreferences {
        val storage = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE)
        return SdoPreferences(
            theme = enumValueOrDefault(storage.getString("theme", null), SdoThemeVariant.CYAN_INDUSTRIAL),
            density = enumValueOrDefault(storage.getString("density", null), SdoContentDensity.COMFORTABLE),
            fontScale = enumValueOrDefault(storage.getString("font_scale", null), SdoFontScale.STANDARD),
            autoSync = storage.getBoolean("auto_sync", true),
            notifications = storage.getBoolean("notifications", true),
            compactCards = storage.getBoolean("compact_cards", false),
            collapseLongSections = storage.getBoolean("collapse_long_sections", true),
            calculationAuditEnabled = storage.getBoolean("calculation_audit_enabled", false),
            showArchivedCampaigns = storage.getBoolean("show_archived_campaigns", true),
        )
    }

    private fun savePreferences(preferences: SdoPreferences) {
        getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE).edit()
            .putString("theme", preferences.theme.name)
            .putString("density", preferences.density.name)
            .putString("font_scale", preferences.fontScale.name)
            .putBoolean("auto_sync", preferences.autoSync)
            .putBoolean("notifications", preferences.notifications)
            .putBoolean("compact_cards", preferences.compactCards)
            .putBoolean("collapse_long_sections", preferences.collapseLongSections)
            .putBoolean("calculation_audit_enabled", preferences.calculationAuditEnabled)
            .putBoolean("show_archived_campaigns", preferences.showArchivedCampaigns)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback

    private companion object {
        const val PREFERENCES_FILE = "sdo_display_preferences"
    }
}
