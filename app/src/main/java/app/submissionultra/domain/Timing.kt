package app.submissionultra.domain

import app.submissionultra.data.Assignment

/**
 * 「最後の開始時刻」まわりの計算。哲学の核となる式をここに一元化する。
 *
 *   最後の開始時刻 = 期限 -（本気でやれば終わる作業時間 + 余裕時間）
 *
 * この時刻を過ぎても未完了なら、Ultra は緊急通知を出す。
 */
object Timing {

    private const val MINUTE_MILLIS = 60_000L

    /** 最後の開始時刻（epoch ミリ秒）。 */
    fun criticalStartMillis(assignment: Assignment, marginMinutes: Int): Long =
        assignment.deadlineEpochMillis - (assignment.effortMinutes + marginMinutes) * MINUTE_MILLIS

    /** 緊急度の分類。UI の並べ替えと控えめな色分けにのみ使う。 */
    fun urgency(
        assignment: Assignment,
        marginMinutes: Int,
        now: Long = System.currentTimeMillis(),
    ): Urgency {
        val critical = criticalStartMillis(assignment, marginMinutes)
        return when {
            now >= assignment.deadlineEpochMillis -> Urgency.OVERDUE
            now >= critical -> Urgency.PAST_START
            else -> Urgency.HAS_TIME
        }
    }
}

/**
 * HAS_TIME:  まだ余裕がある（最後の開始時刻より前）
 * PAST_START: 最後の開始時刻を過ぎた（本来もう着手しているべき）
 * OVERDUE:   期限を過ぎた
 */
enum class Urgency {
    HAS_TIME,
    PAST_START,
    OVERDUE,
}
