package app.submissionultra.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.submissionultra.data.AssignmentRepository
import app.submissionultra.data.ReminderConfig
import app.submissionultra.data.ReminderMode
import app.submissionultra.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val assignmentRepository: AssignmentRepository,
) : ViewModel() {

    val marginMinutes: StateFlow<Int?> =
        settingsRepository.marginMinutes
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val reminderConfig: StateFlow<ReminderConfig?> =
        settingsRepository.reminderConfig
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 余裕時間を変更したら、全課題の最後の開始時刻アラームを引き直す。 */
    fun setMargin(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setMarginMinutes(minutes)
            assignmentRepository.rescheduleAll()
        }
    }

    /** リマインダーの時刻の決め方を変更したら、全課題のリマインダーを引き直す。 */
    fun setReminderMode(mode: ReminderMode) {
        viewModelScope.launch {
            settingsRepository.setReminderMode(mode)
            assignmentRepository.rescheduleAll()
        }
    }

    /** リマインダーの固定時刻を変更したら、全課題のリマインダーを引き直す。 */
    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setReminderTime(hour, minute)
            assignmentRepository.rescheduleAll()
        }
    }
}
