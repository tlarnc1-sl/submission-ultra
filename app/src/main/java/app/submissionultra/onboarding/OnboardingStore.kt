package app.submissionultra.onboarding

import android.content.Context

/**
 * 初回のチュートリアルを見終えたかどうか。
 *
 * DataStore ではなく SharedPreferences なのは、同期で読めるから。DataStore は初回値が
 * 非同期に届くため、一瞬ホーム画面が見えてからチュートリアルが被さってしまう。
 * 同じ理由で先に SharedPreferences を選んだ [app.submissionultra.readiness.OemPowerSettings] に揃える。
 */
object OnboardingStore {

    private const val PREFS = "onboarding"
    private const val KEY_COMPLETED = "completed"

    fun isCompleted(context: Context): Boolean =
        prefs(context).getBoolean(KEY_COMPLETED, false)

    /** 最後まで見た場合もスキップした場合も完了とみなす。以降は設定の「使い方」から見返す。 */
    fun markCompleted(context: Context) {
        prefs(context).edit().putBoolean(KEY_COMPLETED, true).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
