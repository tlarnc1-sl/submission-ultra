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
                checkBatteryOptimization(context),
                checkOverlay(context),
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
            detail = "正確なアラームの権限がありません。最後の開始時刻ちょうどに発火できず、遅れる可能性があります。",
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
            detail = "バッテリー最適化の対象のままだと、アラームが遅延・省略される可能性があります。除外を強く推奨します。",
        )
    }

    private fun checkOverlay(context: Context): ReadinessItem {
        // 全画面を「通知の全画面Intent」に頼らず自前で最前面に出せるか（＝オーバーレイ権限）。
        val granted = EmergencyNotifier.canLaunchAlarm(context)
        return ReadinessItem(
            key = ReadinessKey.OVERLAY,
            satisfied = granted,
            required = false,
            label = "全画面アラームを最前面に表示（推奨）",
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

            ReadinessKey.BATTERY_OPTIMIZATION ->
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$pkg"))

            ReadinessKey.OVERLAY ->
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$pkg"))
        }
    }

    private fun appDetailsIntent(pkg: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
}
