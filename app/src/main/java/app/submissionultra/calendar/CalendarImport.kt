package app.submissionultra.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * 端末のカレンダーに入っている予定を読む。
 *
 * Google Classroom は、期限のある課題を生徒の Google カレンダーへ自動で載せる。
 * カレンダーアプリがそれを端末へ同期しているので、ここを読めば Classroom の課題に
 * 手が届く。アプリ自身は通信しない（INTERNET 権限を持たない）。
 *
 * 同期はしない。ユーザーが「取り込む」を押したときだけ読む。
 * 裏で勝手に同期すると、それが黙って壊れたときに「登録されているはず」と思い込んだまま
 * 提出物を取りこぼす。このアプリで最も避けたい壊れ方がそれなので、
 * 取り込みは必ず本人の操作から始まり、結果がその場で目に見えるようにする。
 */
object CalendarImport {

    /** 候補として見せる期間。これより先の予定は課題として登録するには早すぎる。 */
    private const val WINDOW_DAYS = 60L

    private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

    fun isGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * これから [WINDOW_DAYS] 日以内に始まる予定を、開始が早い順に返す。
     *
     * どれが Classroom の課題かはアプリからは判別できないので、絞り込まずに全部返す。
     * 推測で間引くと、本当に要る予定が黙って消える方が困る。選ぶのは本人に任せる。
     */
    fun upcoming(context: Context, now: Long = System.currentTimeMillis()): List<CalendarItem> {
        if (!isGranted(context)) return emptyList()

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
        )

        val items = mutableListOf<CalendarItem>()
        runCatching {
            CalendarContract.Instances.query(
                context.contentResolver,
                projection,
                now,
                now + WINDOW_DAYS * DAY_MILLIS,
            )
        }.getOrNull()?.use { cursor ->
            while (cursor.moveToNext()) {
                val title = cursor.getString(1)?.trim().orEmpty()
                if (title.isEmpty()) continue

                val allDay = cursor.getInt(3) == 1
                val begin = cursor.getLong(2)
                items += CalendarItem(
                    eventId = cursor.getLong(0),
                    title = title,
                    deadlineMillis = if (allDay) endOfDay(begin) else begin,
                    calendarName = cursor.getString(4)?.trim().orEmpty(),
                    timeKnown = !allDay,
                )
            }
        }
        return items.sortedBy { it.deadlineMillis }
    }

    /**
     * 終日の予定から締切時刻を決める。
     *
     * 終日の予定は UTC の 0 時として記録されるので、まずその日付を UTC で取り出し、
     * 端末のタイムゾーンでその日の 23:59 に置き直す。時刻の指定が無い課題は
     * 「その日のうちに」という意味なので、日の終わりを締切として扱う。
     */
    private fun endOfDay(utcMidnightMillis: Long): Long =
        Instant.ofEpochMilli(utcMidnightMillis)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .atTime(LocalTime.of(23, 59))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
}

/**
 * 取り込みの候補となる予定 1 件。
 *
 * @param timeKnown 終日の予定から 23:59 を補ったのなら false。
 *   締切時刻が推測であることを画面に出すために持つ。
 */
data class CalendarItem(
    val eventId: Long,
    val title: String,
    val deadlineMillis: Long,
    val calendarName: String,
    val timeKnown: Boolean,
)
