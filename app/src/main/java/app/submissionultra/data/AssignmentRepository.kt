package app.submissionultra.data

import app.submissionultra.notification.AlarmScheduler
import app.submissionultra.notification.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * 提出物データの窓口。データ変更のたびに、最後の開始時刻の緊急アラームと、
 * 3日前からのリマインダーの両方を整合させる責務を持つ。
 */
class AssignmentRepository(
    private val dao: AssignmentDao,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler,
    private val reminderScheduler: ReminderScheduler,
) {
    fun observeActive(): Flow<List<Assignment>> = dao.observeActive()

    fun observeAll(): Flow<List<Assignment>> = dao.observeAll()

    fun observeCompleted(): Flow<List<Assignment>> = dao.observeCompleted()

    suspend fun getById(id: Long): Assignment? = dao.getById(id)

    /** 追加または更新し、その課題の緊急アラームとリマインダーを再設定する。 */
    suspend fun upsert(assignment: Assignment) {
        val id = if (assignment.id == 0L) {
            dao.insert(assignment)
        } else {
            dao.update(assignment)
            assignment.id
        }
        val saved = dao.getById(id) ?: return
        scheduleFor(saved)
    }

    /** 完了状態を切り替える。完了ならアラームとリマインダーを止め、未完了に戻せば再設定する。 */
    suspend fun setCompleted(assignment: Assignment, completed: Boolean) {
        val updated = assignment.copy(
            isCompleted = completed,
            completedAtEpochMillis = if (completed) System.currentTimeMillis() else null,
        )
        dao.update(updated)
        scheduleFor(updated)
    }

    suspend fun delete(assignment: Assignment) {
        dao.delete(assignment)
        alarmScheduler.cancel(assignment.id)
        reminderScheduler.cancel(assignment.id)
    }

    /** 余裕時間・リマインダー設定の変更や起動時に、全未完了課題を引き直す。 */
    suspend fun rescheduleAll() {
        val active = dao.getActive()
        val margin = settingsRepository.marginMinutes.first()
        val reminderConfig = settingsRepository.reminderConfig.first()
        alarmScheduler.rescheduleAll(active, margin)
        active.forEach { reminderScheduler.reschedule(it, reminderConfig) }
    }

    /** 1課題分の緊急アラームとリマインダーを設定し直す。 */
    private suspend fun scheduleFor(assignment: Assignment) {
        if (assignment.isCompleted) {
            alarmScheduler.cancel(assignment.id)
            reminderScheduler.cancel(assignment.id)
            return
        }
        val margin = settingsRepository.marginMinutes.first()
        val reminderConfig = settingsRepository.reminderConfig.first()
        alarmScheduler.rescheduleAll(listOf(assignment), margin)
        reminderScheduler.reschedule(assignment, reminderConfig)
    }
}
