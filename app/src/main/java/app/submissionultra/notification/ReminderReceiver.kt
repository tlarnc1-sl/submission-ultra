package app.submissionultra.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.submissionultra.SubmissionUltraApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 締切の3日前からの毎日のリマインダーを AlarmManager から受け取る。
 * 発火時点で未完了、かつ締切前の場合のみ通知する。
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val assignmentId = intent.getLongExtra(NotificationConstants.EXTRA_ASSIGNMENT_ID, -1L)
        if (assignmentId < 0L) return

        val pending = goAsync()
        val app = context.applicationContext as SubmissionUltraApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val assignment = app.graph.assignmentDao.getById(assignmentId)
                if (assignment != null &&
                    !assignment.isCompleted &&
                    System.currentTimeMillis() < assignment.deadlineEpochMillis
                ) {
                    ReminderNotifier.fireReminder(context, assignment)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
