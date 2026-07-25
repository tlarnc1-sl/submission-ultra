package app.submissionultra.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateTime = DateTimeFormatter.ofPattern("M月d日(E) HH:mm")
private val timeOnly = DateTimeFormatter.ofPattern("HH:mm")
private val dateOnly = DateTimeFormatter.ofPattern("yyyy年M月d日")

fun formatDateTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(dateTime)

fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(timeOnly)

/** 「2026年7月25日」のように日付だけを表す。 */
fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(dateOnly)

/**
 * ある時刻までの残りを、常に秒まで刻んで表す（例: `05:30:12` / `2日 05:30:12`）。
 * どの提出物も毎秒動くカウントダウンになるよう、日数が残っていても時分秒を省略しない。
 * 到達済みなら null（呼び出し側で「過ぎています」等の文言を出す）。
 */
fun formatCountdownClock(targetMillis: Long, now: Long = System.currentTimeMillis()): String? {
    val remaining = targetMillis - now
    if (remaining <= 0L) return null
    val totalSeconds = remaining / 1000L
    val days = totalSeconds / 86_400L
    val h = (totalSeconds % 86_400L) / 3_600L
    val m = (totalSeconds % 3_600L) / 60L
    val s = totalSeconds % 60L
    val clock = String.format("%02d:%02d:%02d", h, m, s)
    return if (days > 0L) "${days}日 $clock" else clock
}

/**
 * 期間の長さを「2日3時間」「3時間24分」「24分」のように簡潔に表す（符号は扱わない）。
 * 統計の表示など、秒まで刻む必要がない場所で使う。
 */
fun formatDuration(millis: Long): String {
    val totalMinutes = kotlin.math.abs(millis) / 60_000L
    val days = totalMinutes / 1_440L
    val hours = (totalMinutes % 1_440L) / 60L
    val minutes = totalMinutes % 60L
    return when {
        days > 0L && hours > 0L -> "${days}日${hours}時間"
        days > 0L -> "${days}日"
        hours > 0L && minutes > 0L -> "${hours}時間${minutes}分"
        hours > 0L -> "${hours}時間"
        else -> "${minutes}分"
    }
}

/** 残り時間を「あと2時間30分」「12分経過」のように簡潔に表す。 */
fun formatRelative(targetMillis: Long, now: Long = System.currentTimeMillis()): String {
    val diffMin = (targetMillis - now) / 60_000L
    val overdue = diffMin < 0
    val absMin = kotlin.math.abs(diffMin)
    val hours = absMin / 60
    val minutes = absMin % 60
    val body = when {
        hours > 0 && minutes > 0 -> "${hours}時間${minutes}分"
        hours > 0 -> "${hours}時間"
        else -> "${minutes}分"
    }
    return if (overdue) "${body}経過" else "あと${body}"
}
