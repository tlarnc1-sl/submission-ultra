package app.submissionultra.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.submissionultra.SubmissionUltraApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 設定済みのアラームが OS 側で消える・ずれる出来事を受けて、全部を引き直す。
 *
 * - 再起動: アラームは全て消える。
 * - アプリの更新: OS はそのアプリのアラームを全てキャンセルする。このアプリは APK を
 *   サイトから配って更新するので、これを受けないと更新のたびに、次にアプリを開くまで
 *   全ての提出物が無防備になる。
 * - 時刻・タイムゾーンの変更: 期限は絶対時刻（epoch ミリ秒）で持っているので予定自体は
 *   ずれないが、設定済みのアラームは念のため張り直す。
 */
class RescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED_ACTIONS) return
        rescheduleInBackground(context, goAsync())
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}

/**
 * 未完了の提出物すべてについて、緊急アラームとリマインダーを引き直す。
 *
 * ブロードキャストの処理は本来 onReceive の間しか許されないため、[pending] を握ったまま
 * 非同期に走らせ、終わったら必ず解放する。
 */
internal fun rescheduleInBackground(context: Context, pending: BroadcastReceiver.PendingResult) {
    val app = context.applicationContext as SubmissionUltraApp
    CoroutineScope(Dispatchers.IO).launch {
        try {
            app.graph.assignmentRepository.rescheduleAll()
        } finally {
            pending.finish()
        }
    }
}
