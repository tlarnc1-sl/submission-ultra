package app.submissionultra.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * アラームの見張り。[NotificationConstants.WATCHDOG_INTERVAL_MILLIS] ごとに起きて、
 * 未完了の提出物すべてのアラームを引き直す（[AlarmScheduler.rescheduleAll] が次の見張りも張り直す）。
 *
 * 緊急通知の再発火は「前回の発火に成功したレシーバ」の中でしか次を予約しない鎖になっている。
 * プロセスが落ちる・OS に配信を落とされる・DB の読み出しが間に合わない——どれか一度でも起きれば
 * 鎖は黙って切れ、次にアプリを手で開くまで永久に無音になる。
 *
 * この見張りは鎖の外側にあり、切れていれば繋ぎ直す。ここを通ったこと自体が
 * [AlarmHealth] に記録され、見張りごと止められていた期間は後から検出できる。
 */
class WatchdogReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        rescheduleInBackground(context, goAsync())
    }
}
