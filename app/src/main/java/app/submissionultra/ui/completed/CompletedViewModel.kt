package app.submissionultra.ui.completed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.submissionultra.data.Assignment
import app.submissionultra.data.AssignmentRepository
import app.submissionultra.data.SettingsRepository
import app.submissionultra.domain.Timing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 完了済みの1件。[leadMillis] は「開始リミット（最後の開始時刻）より、どれだけ早く完了できたか」。
 * 正なら早い、負なら開始リミットを過ぎてからの完了。完了時刻が記録されていない古いデータは null。
 */
data class CompletedRow(
    val assignment: Assignment,
    val leadMillis: Long?,
)

/**
 * 完了済みの成績。開始リミットに対してどれだけ余裕を持って終えられているかを表す。
 */
data class CompletedStats(
    /** 平均のリード時間（正なら開始リミットより早く完了）。対象が無ければ null。 */
    val averageLeadMillis: Long?,
    /** 平均の対象になった件数（完了時刻が記録されているもの）。 */
    val ratedCount: Int,
    /** そのうち開始リミットより前に完了できた件数。 */
    val beforeLimitCount: Int,
    /** 完了済みの総数。 */
    val totalCount: Int,
)

class CompletedViewModel(
    private val repository: AssignmentRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    // 画面を開くたびに集計し直すためのトリガ。
    private val refreshKey = MutableStateFlow(0)

    val rows: StateFlow<List<CompletedRow>> =
        combine(
            repository.observeCompleted(),
            settingsRepository.marginMinutes,
            refreshKey,
        ) { assignments, margin, _ ->
            assignments.map { assignment ->
                CompletedRow(
                    assignment = assignment,
                    leadMillis = assignment.completedAtEpochMillis?.let { completedAt ->
                        Timing.criticalStartMillis(assignment, margin) - completedAt
                    },
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats: StateFlow<CompletedStats> =
        rows.map { list ->
            val leads = list.mapNotNull { it.leadMillis }
            CompletedStats(
                averageLeadMillis = if (leads.isEmpty()) null else leads.sum() / leads.size,
                ratedCount = leads.size,
                beforeLimitCount = leads.count { it > 0L },
                totalCount = list.size,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CompletedStats(null, 0, 0, 0),
        )

    /** 画面を開いたときに呼ぶ。最新のデータと余裕時間で集計し直す。 */
    fun refresh() {
        refreshKey.value += 1
    }

    /** 未完了に戻す。緊急アラーム・リマインダーも自動で再設定される。 */
    fun restore(assignment: Assignment) {
        viewModelScope.launch { repository.setCompleted(assignment, false) }
    }

    /** 履歴から完全に削除する。 */
    fun delete(assignment: Assignment) {
        viewModelScope.launch { repository.delete(assignment) }
    }
}
