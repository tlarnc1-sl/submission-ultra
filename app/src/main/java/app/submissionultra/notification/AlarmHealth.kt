package app.submissionultra.notification

import android.content.Context

/**
 * アラームが実際に生きていたかの記録。
 *
 * このアプリの弱点は「アラームが消えたことに誰も気づけない」ことにある。強制停止、
 * OEM 独自の省電力、アプリの更新——どれもアラームを黙って消し、消えたこと自体は
 * どの API からも読めない。
 *
 * そこで、アラームを引き直すたびに時刻を記録し、前回からの間隔を残す。見張り
 * （[NotificationConstants.WATCHDOG_INTERVAL_MILLIS] 間隔）が生きていれば間隔は短いままで、
 * 大きく空いていればその間アプリは止められていた、と後から言える。
 *
 * 記録するだけで自動では直せない。直せない代わりに、[ReadinessChecker] が
 * 「一度アラームが止まっていた」事実をユーザーに見せる。
 */
object AlarmHealth {

    private const val PREFS = "alarm_health"
    private const val KEY_LAST_SCHEDULED_AT = "last_scheduled_at"
    private const val KEY_LAST_GAP = "last_gap"
    private const val KEY_LAST_INTERRUPTION_AT = "last_interruption_at"

    /**
     * 「止められていた」と報せ続ける期間。
     *
     * 間隔そのものを見ると、アプリを開いた瞬間の引き直しで即座に短い値へ上書きされ、
     * 警告が一瞬で消えてしまう。起きた事実の方を残して、しばらく見せ続ける。
     */
    private const val INTERRUPTION_REPORT_WINDOW_MILLIS = 7L * 24 * 60 * 60 * 1000

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * 全体を引き直したことを記録し、前回からの間隔を残す。
     *
     * 初回（記録が無い）は間隔を 0 として扱う。インストール直後を「止まっていた」と
     * 誤って責めないため。
     */
    fun recordReschedule(context: Context) {
        val now = System.currentTimeMillis()
        val last = prefs(context).getLong(KEY_LAST_SCHEDULED_AT, 0L)
        val gap = if (last == 0L) 0L else (now - last).coerceAtLeast(0L)
        val editor = prefs(context).edit()
            .putLong(KEY_LAST_SCHEDULED_AT, now)
            .putLong(KEY_LAST_GAP, gap)
        // 見張りが動いていれば間隔は数時間で収まる。それを大きく超えていたということは、
        // 見張りごと止められていたということ。起きた事実として時刻を残す。
        if (gap > NotificationConstants.ALARM_STALE_THRESHOLD_MILLIS) {
            editor.putLong(KEY_LAST_INTERRUPTION_AT, now)
        }
        editor.apply()
    }

    /** 直近の引き直しの間隔（ミリ秒）。記録が無ければ 0。 */
    fun lastGapMillis(context: Context): Long =
        prefs(context).getLong(KEY_LAST_GAP, 0L)

    /** 最近「アプリが長時間止められていた」ことが観測されたか。 */
    fun wasInterrupted(context: Context): Boolean {
        val at = prefs(context).getLong(KEY_LAST_INTERRUPTION_AT, 0L)
        if (at == 0L) return false
        return System.currentTimeMillis() - at < INTERRUPTION_REPORT_WINDOW_MILLIS
    }
}
