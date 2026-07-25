package app.submissionultra.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.submissionultra.data.Assignment
import app.submissionultra.data.AssignmentRepository
import app.submissionultra.data.SettingsRepository
import app.submissionultra.domain.Timing
import app.submissionultra.domain.Urgency
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeItem(
    val assignment: Assignment,
    val criticalStartMillis: Long,
    val urgency: Urgency,
)

class HomeViewModel(
    private val repository: AssignmentRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    // 緊急度の切り替わり（開始リミット超過・期限超過）と並び順を追従させるための再計算。
    // 秒のカウントダウン自体は CountdownDisplay が自前で刻む。ここで毎秒新しいリストを作っても、
    // 中身が前回と等しい限り StateFlow は通知しない（＝表示は変わらない）ため。
    private val ticker = flow {
        while (true) {
            emit(Unit)
            delay(TICK_INTERVAL_MILLIS)
        }
    }

    val items: StateFlow<List<HomeItem>> =
        combine(
            repository.observeActive(),
            settingsRepository.marginMinutes,
            ticker,
        ) { assignments, margin, _ ->
            val now = System.currentTimeMillis()
            assignments
                .map { assignment ->
                    HomeItem(
                        assignment = assignment,
                        criticalStartMillis = Timing.criticalStartMillis(assignment, margin),
                        urgency = Timing.urgency(assignment, margin, now),
                    )
                }
                // 最後の開始時刻が近い順（過ぎているものが先頭）に並べる。
                .sortedBy { it.criticalStartMillis }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun markCompleted(assignment: Assignment) {
        viewModelScope.launch { repository.setCompleted(assignment, true) }
    }

    /** 完了の取り消し。緊急アラーム・リマインダーも自動で復帰する。 */
    fun undoComplete(assignment: Assignment) {
        viewModelScope.launch { repository.setCompleted(assignment, false) }
    }

    private companion object {
        // 締切1日前からは秒までカウントダウンするため毎秒更新する。
        const val TICK_INTERVAL_MILLIS = 1_000L
    }
}
