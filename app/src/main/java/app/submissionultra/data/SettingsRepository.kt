package app.submissionultra.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * グローバル設定。余裕時間（緊急通知用）と、リマインダー通知の時刻設定を持つ。
 */
class SettingsRepository(private val context: Context) {

    val marginMinutes: Flow<Int> =
        context.dataStore.data.map { it[MARGIN_MINUTES] ?: DEFAULT_MARGIN_MINUTES }

    suspend fun setMarginMinutes(minutes: Int) {
        context.dataStore.edit { it[MARGIN_MINUTES] = minutes.coerceAtLeast(0) }
    }

    val reminderConfig: Flow<ReminderConfig> =
        context.dataStore.data.map { prefs ->
            val mode = when (prefs[REMINDER_MODE]) {
                ReminderMode.DEADLINE.name -> ReminderMode.DEADLINE
                else -> ReminderMode.FIXED
            }
            ReminderConfig(
                mode = mode,
                hour = prefs[REMINDER_HOUR] ?: ReminderConfig.DEFAULT_HOUR,
                minute = prefs[REMINDER_MINUTE] ?: ReminderConfig.DEFAULT_MINUTE,
            )
        }

    suspend fun setReminderMode(mode: ReminderMode) {
        context.dataStore.edit { it[REMINDER_MODE] = mode.name }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[REMINDER_HOUR] = hour.coerceIn(0, 23)
            it[REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    companion object {
        const val DEFAULT_MARGIN_MINUTES = 30
        private val MARGIN_MINUTES = intPreferencesKey("margin_minutes")
        private val REMINDER_MODE = stringPreferencesKey("reminder_mode")
        private val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        private val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
    }
}
