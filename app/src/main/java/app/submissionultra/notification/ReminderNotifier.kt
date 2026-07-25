package app.submissionultra.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.submissionultra.MainActivity
import app.submissionultra.R
import app.submissionultra.data.Assignment
import app.submissionultra.data.deadlineLabel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

/**
 * リマインダー通知。緊急通知とは別レベル・別チャンネル。
 * DND をバイパスせず、重要度 DEFAULT（通常の通知音）。マナーモード・おやすみモードを尊重する。
 * 緊急通知の唯一経路（[EmergencyNotifier.fireEmergencyNotification]）とは意図的に分離している。
 */
object ReminderNotifier {

    private val dateFormatter = DateTimeFormatter.ofPattern("M月d日(E) HH:mm")

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(NotificationConstants.REMINDER_CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            NotificationConstants.REMINDER_CHANNEL_ID,
            NotificationConstants.REMINDER_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "締切の3日前から、未完了の提出物を毎日知らせます。"
            // DND はバイパスしない。通常の通知音・マナーモードに従う。
        }
        manager.createNotificationChannel(channel)
    }

    fun fireReminder(context: Context, assignment: Assignment) {
        ensureChannel(context)

        val now = System.currentTimeMillis()
        val deadlineText = Instant.ofEpochMilli(assignment.deadlineEpochMillis)
            .atZone(ZoneId.systemDefault())
            .format(dateFormatter)
        val remaining = remainingText(assignment.deadlineEpochMillis, now)
        val deadlineLabel = assignment.type.deadlineLabel()

        val contentIntent = PendingIntent.getActivity(
            context,
            reminderNotificationId(assignment.id),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, NotificationConstants.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_submission)
            .setContentTitle("未提出の提出物があります")
            .setContentText("${assignment.title}  $remaining")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${assignment.title}\n$deadlineLabel $deadlineText（$remaining）。まだ未提出です。"),
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(reminderNotificationId(assignment.id), notification)
    }

    private fun remainingText(deadlineMillis: Long, now: Long): String {
        val diff = deadlineMillis - now
        if (diff <= 0) return "時間切れです"
        val days = ceil(diff / (24.0 * 60 * 60 * 1000)).toInt()
        return if (days <= 1) "本日中" else "あと${days}日"
    }

    /** 緊急通知の ID（= assignment.id）と衝突しないよう、オフセットを足した ID を使う。 */
    private fun reminderNotificationId(assignmentId: Long): Int =
        (NotificationConstants.REMINDER_NOTIFICATION_OFFSET + assignmentId).toInt()
}
