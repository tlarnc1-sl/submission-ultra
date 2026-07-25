package app.submissionultra.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.submissionultra.SubmissionUltraApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 端末再起動でアラームは全て消えるため、起動後に全ての未完了提出物の
 * 緊急アラームとリマインダーを再設定する。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pending = goAsync()
        val app = context.applicationContext as SubmissionUltraApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.graph.assignmentRepository.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
