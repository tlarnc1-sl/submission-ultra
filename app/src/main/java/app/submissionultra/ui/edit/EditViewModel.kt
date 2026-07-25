package app.submissionultra.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.submissionultra.data.Assignment
import app.submissionultra.data.AssignmentRepository
import app.submissionultra.data.AssignmentType
import app.submissionultra.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

data class EditState(
    val id: Long = 0L,
    val title: String = "",
    val type: AssignmentType = AssignmentType.CLASSROOM,
    val deadlineMillis: Long = defaultDeadline(),
    val effortText: String = "",
    val teacherName: String = "",
    val marginMinutes: Int = SettingsRepository.DEFAULT_MARGIN_MINUTES,
    val isEditing: Boolean = false,
    val loaded: Boolean = false,
) {
    val effortMinutes: Int? = effortText.toIntOrNull()?.takeIf { it > 0 }

    val canSave: Boolean = title.isNotBlank() && effortMinutes != null

    /** 現在の入力から算出した「最後の開始時刻」プレビュー。作業時間が未入力なら null。 */
    val criticalStartPreviewMillis: Long? =
        effortMinutes?.let { deadlineMillis - (it + marginMinutes) * 60_000L }
}

private fun defaultDeadline(): Long =
    LocalDateTime.now()
        .plusDays(1)
        .withHour(23).withMinute(59).withSecond(0).withNano(0)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

class EditViewModel(
    private val repository: AssignmentRepository,
    settingsRepository: SettingsRepository,
    private val assignmentId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(EditState(isEditing = assignmentId != 0L))
    val state: StateFlow<EditState> = _state.asStateFlow()

    init {
        // 余裕時間はプレビュー計算に使う。変更されたら追従する。
        viewModelScope.launch {
            settingsRepository.marginMinutes.collect { margin ->
                _state.value = _state.value.copy(marginMinutes = margin)
            }
        }

        if (assignmentId != 0L) {
            viewModelScope.launch {
                repository.getById(assignmentId)?.let { existing ->
                    _state.value = _state.value.copy(
                        id = existing.id,
                        title = existing.title,
                        type = existing.type,
                        deadlineMillis = existing.deadlineEpochMillis,
                        effortText = existing.effortMinutes.toString(),
                        teacherName = existing.teacherName ?: "",
                        isEditing = true,
                        loaded = true,
                    )
                }
            }
        } else {
            _state.value = _state.value.copy(loaded = true)
        }
    }

    fun onTitleChange(value: String) { _state.value = _state.value.copy(title = value) }
    fun onTypeChange(value: AssignmentType) { _state.value = _state.value.copy(type = value) }
    fun onEffortChange(value: String) {
        _state.value = _state.value.copy(effortText = value.filter { it.isDigit() }.take(4))
    }
    fun onTeacherChange(value: String) { _state.value = _state.value.copy(teacherName = value) }
    fun onDeadlineChange(millis: Long) { _state.value = _state.value.copy(deadlineMillis = millis) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            repository.upsert(
                Assignment(
                    id = s.id,
                    title = s.title.trim(),
                    type = s.type,
                    deadlineEpochMillis = s.deadlineMillis,
                    effortMinutes = s.effortMinutes ?: return@launch,
                    // 編集時は完了状態を保持したいが、ホームからは未完了のみ編集するため false 固定でよい。
                    isCompleted = false,
                    teacherName = s.teacherName.trim().ifBlank { null },
                ),
            )
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val s = _state.value
        if (s.id == 0L) { onDone(); return }
        viewModelScope.launch {
            repository.getById(s.id)?.let { repository.delete(it) }
            onDone()
        }
    }
}
