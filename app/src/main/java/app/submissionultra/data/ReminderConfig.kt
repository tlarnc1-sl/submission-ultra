package app.submissionultra.data

/**
 * リマインダー通知を鳴らす時刻の決め方。
 * FIXED:    毎回決まった時刻（[ReminderConfig.hour]:[ReminderConfig.minute]）に鳴らす。
 * DEADLINE: 締切と同じ時刻に鳴らす（3日前・2日前・1日前の同時刻）。
 */
enum class ReminderMode {
    FIXED,
    DEADLINE,
}

/**
 * リマインダー通知のグローバル設定。全課題共通。
 * hour/minute は FIXED モードのときの時刻。DEADLINE モードでは使わない。
 */
data class ReminderConfig(
    val mode: ReminderMode,
    val hour: Int,
    val minute: Int,
) {
    companion object {
        const val DEFAULT_HOUR = 20
        const val DEFAULT_MINUTE = 0
        /** 締切の何日前からリマインダーを開始するか。 */
        const val DAYS_BEFORE = 3

        val DEFAULT = ReminderConfig(ReminderMode.FIXED, DEFAULT_HOUR, DEFAULT_MINUTE)
    }
}
