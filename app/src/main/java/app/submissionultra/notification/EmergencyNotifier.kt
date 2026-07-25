package app.submissionultra.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.submissionultra.MainActivity
import app.submissionultra.R
import app.submissionultra.data.Assignment
import app.submissionultra.ui.alarm.EmergencyAlarmActivity
import app.submissionultra.data.deadlineLabel
import app.submissionultra.domain.Timing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 緊急通知の唯一の発火経路。
 *
 * 本番（AlarmReceiver 経由）もテスト（設定画面のボタン経由）も、必ずこの
 * [fireEmergencyNotification] を通る。渡す Assignment がテスト用かどうかだけが違いで、
 * チャンネル・音・振動・DND 例外・表示レイアウトはすべて同一のコードパスを通る。
 * テストの成功は、その端末設定で本番の緊急通知が動くことの証明でなければならない。
 */
object EmergencyNotifier {

    private val timeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")

    // 注意: ここの「チャンネル版(emergency_vN)」はアプリの versionName/versionCode とは無関係。
    // DND バイパスを効かせるために内部的に作り直す通知チャンネルの世代番号にすぎない。
    private const val PREFS = "emergency_channel"
    private const val KEY_VERSION = "version"
    private const val KEY_CREATED_WITH_DND_ACCESS = "created_with_dnd_access"
    private const val KEY_ACKNOWLEDGED_PREFIX = "acknowledged_"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 現在有効な緊急通知チャンネルの ID（"emergency_v1" など）。 */
    fun currentChannelId(context: Context): String =
        NotificationConstants.EMERGENCY_CHANNEL_ID_PREFIX + prefs(context).getInt(KEY_VERSION, 1)

    /**
     * 緊急通知チャンネルを用意する。IMPORTANCE_HIGH + bypassDnd + アラーム音 + 振動。
     *
     * 核心: `setBypassDnd(true)` は「DND ポリシーアクセスを持った状態でチャンネルを新規作成した」
     * ときにしか効かない。初回起動はアクセス前のことが多く、その場合バイパス無しで作られる。
     * かつ、同じ ID を削除→再作成しても、システムが古い設定（バイパス無し）を復活させるため直らない。
     *
     * そこで、アクセスを取得済みなのに現行チャンネルがバイパスできていない場合は、
     * **新しいバージョン ID で一度だけ作り直す**（新 ID には過去設定が無いのでバイパスが確実に適用される）。
     * ユーザーが手動でバイパスを有効化済み（canBypassDnd()==true）の場合は何もしない。
     */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val hasDndAccess = manager.isNotificationPolicyAccessGranted

        val id = currentChannelId(context)
        val existing = manager.getNotificationChannel(id)

        if (existing == null) {
            manager.createNotificationChannel(buildChannel(context, id))
            prefs(context).edit().putBoolean(KEY_CREATED_WITH_DND_ACCESS, hasDndAccess).apply()
            return
        }

        // 既にバイパスできているなら何もしない（ユーザーの設定も尊重）。
        if (existing.canBypassDnd()) return

        // バイパスできていない。アクセスが取得済みで、かつ「アクセス前に作られたチャンネル」の場合だけ、
        // 新バージョン ID で一度だけ作り直す。1回で止まる（無限ループ・チャンネル乱造を防ぐ）。
        val createdWithAccess = prefs(context).getBoolean(KEY_CREATED_WITH_DND_ACCESS, false)
        if (hasDndAccess && !createdWithAccess) {
            val newVersion = prefs(context).getInt(KEY_VERSION, 1) + 1
            prefs(context).edit()
                .putInt(KEY_VERSION, newVersion)
                .putBoolean(KEY_CREATED_WITH_DND_ACCESS, true)
                .apply()
            val newId = NotificationConstants.EMERGENCY_CHANNEL_ID_PREFIX + newVersion
            manager.createNotificationChannel(buildChannel(context, newId))
            manager.deleteNotificationChannel(id)
        }
    }

    /**
     * 現行の緊急チャンネルが「重要度 HIGH かつ DND バイパスが効く」状態か。
     *
     * 注意: `canBypassDnd()` は一部の OEM ROM（ColorOS など）で信頼できない。
     * バイパスは実際に効いている（DND 中に鳴る）のに、アプリ再起動後に false を返すことがある。
     * このアプリはチャンネルを常にバイパス付きで作るため、DND ポリシーアクセスが許可されていれば
     * 実際にはバイパスする。よって「canBypassDnd() が true、または DND アクセスが許可済み」を
     * バイパス成立とみなす（偽陰性で「鳴らない」と誤表示しないため）。
     */
    fun isEmergencyChannelReady(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = manager.getNotificationChannel(currentChannelId(context)) ?: return false
        if (channel.importance < NotificationManager.IMPORTANCE_HIGH) return false
        return channel.canBypassDnd() || manager.isNotificationPolicyAccessGranted
    }

    private fun buildChannel(context: Context, id: String): NotificationChannel {
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        return NotificationChannel(
            id,
            NotificationConstants.EMERGENCY_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "最後の開始時刻を過ぎた未完了の提出物を、時刻・マナーモードに関係なく知らせます。"
            setBypassDnd(true)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
            setSound(alarmSound, audioAttributes)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
    }

    /**
     * 緊急通知を発火する唯一の関数。本番・テスト共通。
     * 呼び出し前に通知権限が無い場合、システム側で表示されないだけで分岐はしない。
     */
    fun fireEmergencyNotification(context: Context, assignment: Assignment) {
        ensureChannel(context)

        val deadline = Instant.ofEpochMilli(assignment.deadlineEpochMillis)
            .atZone(ZoneId.systemDefault())
            .format(timeFormatter)
        val deadlineLabel = assignment.type.deadlineLabel()

        // 本文タップ：通常どおりアプリを開く。どの課題から来たかを渡し、
        // 起動時の rescheduleAll でアラーム画面に引き戻されないようにする。
        val contentIntent = PendingIntent.getActivity(
            context,
            assignment.id.toInt(),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(NotificationConstants.EXTRA_ASSIGNMENT_ID, assignment.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // 全画面Intent：鳴った瞬間に専用のアラーム画面を出す（ロック画面上でも表示）。
        // ただしOSは「ロック中/画面オフ」以外や通知が溜まっている時は発動しないことがある。
        // その穴は下の maybeLaunchAlarm（オーバーレイ権限で自前起動）で埋める。
        val fullScreenIntent = PendingIntent.getActivity(
            context,
            assignment.id.toInt(),
            buildAlarmIntent(context, assignment),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // 「完了にする」アクション：通知から直接完了。
        val completeIntent = PendingIntent.getBroadcast(
            context,
            assignment.id.toInt(),
            Intent(context, EmergencyActionReceiver::class.java).apply {
                action = NotificationConstants.ACTION_COMPLETE
                putExtra(NotificationConstants.EXTRA_ASSIGNMENT_ID, assignment.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        // 期限を過ぎた後も「今始めれば間に合う」と言い続けないよう、文言を切り替える。
        val overdue = Timing.isOverdue(assignment)
        val title = if (overdue) "期限を過ぎています" else "今すぐ始めないと間に合わない"
        val body = if (overdue) {
            "${assignment.title}\n${deadlineLabel}の $deadline を過ぎました。まだ提出できていません。"
        } else {
            "${assignment.title}\n最後の開始時刻を過ぎました。${deadlineLabel}は $deadline です。今始めれば間に合います。"
        }

        val notification = NotificationCompat.Builder(context, currentChannelId(context))
            .setSmallIcon(R.drawable.ic_stat_submission)
            .setContentTitle(title)
            .setContentText("${assignment.title} $deadlineLabel $deadline")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmSound, AudioAttributes.USAGE_ALARM)
            .setVibrate(longArrayOf(0, 500, 250, 500, 250, 500))
            .setContentIntent(contentIntent)
            .setFullScreenIntent(fullScreenIntent, true)
            .addAction(0, "完了にする", completeIntent)
            // スワイプで消して無かったことにできないようにする。消えるのは
            // 「完了にする」か、本文タップでアプリを開いたときだけ。
            .setOngoing(true)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(emergencyNotificationId(assignment.id), notification)

        // 通知の全画面Intentに頼らず、可能なら自分で全画面を前面に出す。
        // これで「通知が数件溜まっている」「画面ロック解除中」でも確実に全画面が出る。
        maybeLaunchAlarm(context, assignment)
    }

    /** 全画面アラーム画面(EmergencyAlarmActivity)を開くための Intent（表示用 extras 付き）。 */
    private fun buildAlarmIntent(context: Context, assignment: Assignment): Intent =
        Intent(context, EmergencyAlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(NotificationConstants.EXTRA_ASSIGNMENT_ID, assignment.id)
            putExtra(NotificationConstants.EXTRA_TITLE, assignment.title)
            putExtra(NotificationConstants.EXTRA_DEADLINE_MILLIS, assignment.deadlineEpochMillis)
            putExtra(NotificationConstants.EXTRA_TYPE, assignment.type.name)
            putExtra(NotificationConstants.EXTRA_TEACHER, assignment.teacherName)
        }

    /**
     * 「他のアプリの上に表示」(SYSTEM_ALERT_WINDOW / overlay)権限があれば、バックグラウンドからでも
     * アラーム画面を前面に起動できる（ロック解除中・通知が溜まっている時も確実）。無ければ何もせず、
     * ロック中は通知の全画面Intentに委ねる。
     */
    private fun maybeLaunchAlarm(context: Context, assignment: Assignment) {
        if (canLaunchAlarm(context)) {
            runCatching { context.startActivity(buildAlarmIntent(context, assignment)) }
        }
    }

    /** バックグラウンドから全画面を自前起動できるか（オーバーレイ権限の有無）。 */
    fun canLaunchAlarm(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    /** 緊急通知の ID。レシーバー/アラーム画面から同じ通知をキャンセルするために公開する。 */
    fun emergencyNotificationId(assignmentId: Long): Int = assignmentId.toInt()

    /**
     * アラーム画面の「開く」で作業に入ったことを記録する。
     *
     * 「開く」はアプリを起動し、起動時の rescheduleAll は開始時刻を過ぎた課題をその場で鳴らす。
     * そのままだとアラーム画面に戻され、二度と作業に入れない無限ループになるため、
     * 直後の再発火だけを抑える。抑制中も再通知の予約は生きているので、放置すれば必ずまた鳴る。
     */
    fun acknowledge(context: Context, assignmentId: Long) {
        prefs(context).edit()
            .putLong(KEY_ACKNOWLEDGED_PREFIX + assignmentId, System.currentTimeMillis())
            .apply()
    }

    /** 直近に「開く」で作業に入ったか（＝今この瞬間の再発火を抑えるべきか）。 */
    fun isRecentlyAcknowledged(context: Context, assignmentId: Long): Boolean {
        val at = prefs(context).getLong(KEY_ACKNOWLEDGED_PREFIX + assignmentId, 0L)
        return System.currentTimeMillis() - at < NotificationConstants.EMERGENCY_RETRY_INTERVAL_MILLIS
    }
}
