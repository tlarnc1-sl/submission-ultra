package app.submissionultra.readiness

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import app.submissionultra.notification.AlarmHealth
import app.submissionultra.notification.EmergencyNotifier

/**
 * 起動のたびに、緊急通知に必要な端末状態を確認する。
 * 権限は「一度許可されたら永遠」ではないため、信頼せず毎回確認する。
 */
object ReadinessChecker {

    fun check(context: Context): ReadinessReport {
        return ReadinessReport(
            listOf(
                checkNotifications(context),
                checkExactAlarm(context),
                checkDndAccess(context),
                checkEmergencyChannel(context),
                checkFullScreenIntent(context),
                checkOverlay(context),
                checkBatteryOptimization(context),
                checkAlarmDelivery(context),
            ),
        )
    }

    private fun checkNotifications(context: Context): ReadinessItem {
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        return ReadinessItem(
            key = ReadinessKey.NOTIFICATIONS,
            satisfied = enabled,
            required = true,
            label = "通知を表示できる",
            detail = "通知が許可されていません。このままでは緊急通知を表示できません。",
        )
    }

    private fun checkExactAlarm(context: Context): ReadinessItem {
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        } else {
            true
        }
        return ReadinessItem(
            key = ReadinessKey.EXACT_ALARM,
            satisfied = canExact,
            required = true,
            label = "正確な時刻に鳴らせる",
            detail = "正確なアラームの権限がありません。最後の開始時刻ちょうどに発火できず、" +
                "画面を消している間は数十分から数時間ずれることがあります。",
        )
    }

    /**
     * 通知の全画面 Intent が使えるか。
     *
     * Android 14 以降は既定で拒否されることがあり、拒否されると全画面アラームが
     * ただのヘッドアップ通知に格下げされる。音は鳴るので、確認しないと
     * 「なぜか全画面が出ない」という形でしか気づけない。
     */
    private fun checkFullScreenIntent(context: Context): ReadinessItem {
        val granted = EmergencyNotifier.canUseFullScreenIntent(context)
        return ReadinessItem(
            key = ReadinessKey.FULL_SCREEN_INTENT,
            satisfied = granted,
            required = true,
            label = "ロック画面に全画面で割り込める",
            detail = "全画面通知が許可されていません。このままでは音は鳴っても、" +
                "画面いっぱいのアラームにはならず、通知が一枚出るだけになります。",
        )
    }

    /**
     * 直近にアプリが長時間止められていた痕跡があるか。
     *
     * 権限ではなく、起きてしまったことの報告。強制停止や OEM の省電力でアラームごと
     * 消された期間があったことは、この記録からしか分からない。
     */
    private fun checkAlarmDelivery(context: Context): ReadinessItem {
        return ReadinessItem(
            key = ReadinessKey.ALARM_DELIVERY,
            satisfied = !AlarmHealth.wasInterrupted(context),
            required = false,
            label = "アラームが止められていない",
            detail = "この 1 週間のうちに、アプリが長時間止められていた形跡があります。その間、緊急通知は鳴りませんでした。" +
                "アプリを強制停止しないでください。端末の設定で「自動起動」を許可すると起きにくくなります。",
        )
    }

    private fun checkDndAccess(context: Context): ReadinessItem {
        val granted = context.getSystemService(NotificationManager::class.java)
            .isNotificationPolicyAccessGranted
        return ReadinessItem(
            key = ReadinessKey.DND_ACCESS,
            satisfied = granted,
            required = true,
            label = "おやすみモード中でも鳴らせる",
            detail = "おやすみモードへのアクセスが未許可です。これが無いと、緊急チャンネルの「DND 例外」も有効になりません。",
        )
    }

    private fun checkEmergencyChannel(context: Context): ReadinessItem {
        val satisfied = EmergencyNotifier.isEmergencyChannelReady(context)
        return ReadinessItem(
            key = ReadinessKey.EMERGENCY_CHANNEL,
            satisfied = satisfied,
            required = true,
            label = "緊急チャンネルがサイレント例外になっている",
            detail = "緊急通知チャンネルが無効化されているか、重要度が下げられているか、DND 例外が有効になっていません。" +
                "先に「おやすみモード中でも鳴らせる」を許可すると、この項目も自動で有効になります。",
        )
    }

    private fun checkBatteryOptimization(context: Context): ReadinessItem {
        val ignoring = context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName)
        return ReadinessItem(
            key = ReadinessKey.BATTERY_OPTIMIZATION,
            satisfied = ignoring,
            required = false,
            label = "バッテリー最適化から除外されている（推奨）",
            detail = "アラームは目覚まし時計として予約しているのでこの設定に左右されませんが、" +
                "除外しておくと、鳴った後の処理まで確実に走ります。",
        )
    }

    /**
     * 全画面を「通知の全画面 Intent」に頼らず自前で最前面に出せるか（＝オーバーレイ権限）。
     *
     * 必須にしている理由: OS は画面が点いている時や通知が数件溜まっている時、全画面 Intent を
     * 発動させないことがある。そのときアラーム画面を出せる手段はこれしか残らない。
     * 「音は鳴ったが画面は出なかった」を許さないなら、これは推奨ではなく必須になる。
     */
    private fun checkOverlay(context: Context): ReadinessItem {
        val granted = EmergencyNotifier.canLaunchAlarm(context)
        return ReadinessItem(
            key = ReadinessKey.OVERLAY,
            satisfied = granted,
            required = true,
            label = "全画面アラームを最前面に表示できる",
            detail = "「他のアプリの上に表示」が未許可です。通知が数件溜まっている時や画面ロック解除中に、" +
                "全画面アラームが出ないことがあります（音・通知は出ます）。",
        )
    }

    /**
     * 各項目に対応するシステム設定画面を開く Intent。「今すぐ設定する」導線に使う。
     */
    fun settingsIntent(context: Context, key: ReadinessKey): Intent {
        val pkg = context.packageName
        return when (key) {
            ReadinessKey.NOTIFICATIONS ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, pkg)
                } else {
                    appDetailsIntent(pkg)
                }

            ReadinessKey.EXACT_ALARM ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$pkg"))
                } else {
                    appDetailsIntent(pkg)
                }

            ReadinessKey.DND_ACCESS ->
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

            ReadinessKey.EMERGENCY_CHANNEL ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, pkg)
                        .putExtra(Settings.EXTRA_CHANNEL_ID, EmergencyNotifier.currentChannelId(context))
                } else {
                    appDetailsIntent(pkg)
                }

            ReadinessKey.FULL_SCREEN_INTENT ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    Intent(
                        Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        Uri.parse("package:$pkg"),
                    )
                } else {
                    appDetailsIntent(pkg)
                }

            ReadinessKey.BATTERY_OPTIMIZATION ->
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$pkg"))

            ReadinessKey.OVERLAY ->
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$pkg"))

            // 権限ではないので設定先が無い。強制停止も自動起動もアプリの詳細画面が出発点になる。
            ReadinessKey.ALARM_DELIVERY -> appDetailsIntent(pkg)
        }
    }

    private fun appDetailsIntent(pkg: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
}
