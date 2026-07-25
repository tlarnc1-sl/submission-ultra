package app.submissionultra.readiness

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * 独自の省電力機能でバックグラウンドのアプリを止める OS の検出と、その設定画面への導線。
 *
 * 日本で広く使われている中で実害が出るのは Xiaomi 系（HyperOS / MIUI）と ColorOS 系
 * （Oppo・realme・OnePlus）。これらは Android 標準の「バッテリー最適化の除外」「正確なアラーム」
 * 「他のアプリの上に表示」をすべて許可していても、OS 独自の「自動起動」制限によって
 * 画面を消している間のアラーム配信が遅れる。
 *
 * この制限は公開 API から状態を読むことも、プログラムから許可することもできない。
 * できるのは「検出して警告し、設定画面まで案内する」ことだけ。だからこの項目だけは
 * 「満たされているか」ではなく「ユーザーが対処に進んだか」で扱う。
 */
object OemPowerSettings {

    /** 自動起動の許可が別途必要な OS。 */
    enum class RestrictiveOs(val displayName: String) {
        HYPER_OS("HyperOS"),
        MIUI("MIUI"),
        COLOR_OS("ColorOS"),
    }

    private const val PREFS = "oem_power"
    private const val KEY_WARNING_SHOWN = "warning_shown"

    /**
     * この端末の OS。該当しなければ null。
     *
     * 端末が途中で変わることはないので、プロセスごとに一度だけ判定して使い回す
     * （[ReadinessChecker] は前面復帰のたびに走るため、毎回 getprop を叩かせない）。
     */
    val current: RestrictiveOs? by lazy { detect() }

    /**
     * この端末の OS 名。制限の有無に関わらず、分かる範囲で名乗らせる。
     * 判別できないメーカーでは素の "Android" として扱う（誤った名前を出さない）。
     */
    val osName: String by lazy { romName() ?: "Android" }

    /** OS 名だけでは分からない、メーカーと Android のバージョン。 */
    val osDetail: String by lazy {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        "$manufacturer ・ Android ${Build.VERSION.RELEASE}"
    }

    private fun romName(): String? = when (Build.MANUFACTURER.lowercase()) {
        "xiaomi" -> when {
            systemProperty("ro.mi.os.version.name").isNotEmpty() -> "HyperOS"
            systemProperty("ro.miui.ui.version.name").isNotEmpty() -> "MIUI"
            else -> null
        }

        "oppo", "realme", "oneplus" -> "ColorOS"
        "samsung" -> "One UI"
        "vivo", "iqoo" -> "OriginOS"
        "huawei" -> "HarmonyOS"
        "honor" -> "MagicOS"
        else -> null
    }

    private fun detect(): RestrictiveOs? = when (Build.MANUFACTURER.lowercase()) {
        // Redmi・POCO も MANUFACTURER は Xiaomi。HyperOS は MIUI の後継で、
        // 名乗るプロパティが違うだけで自動起動制限は同じ。
        "xiaomi" -> when {
            systemProperty("ro.mi.os.version.name").isNotEmpty() -> RestrictiveOs.HYPER_OS
            systemProperty("ro.miui.ui.version.name").isNotEmpty() -> RestrictiveOs.MIUI
            else -> null
        }

        // realme・OnePlus も ColorOS 系（OxygenOS は現在 ColorOS ベース）。
        "oppo", "realme", "oneplus" -> RestrictiveOs.COLOR_OS

        else -> null
    }

    /** 初回起動時の警告を出すべきか（該当端末で、まだ一度も出していない）。 */
    fun shouldWarn(context: Context): Boolean =
        current != null && !prefs(context).getBoolean(KEY_WARNING_SHOWN, false)

    /**
     * 警告を出したことを記録する。割り込むのは最初の一度きり。
     *
     * 実際に許可したかは確認できないので「対処済み」とは記録しない。以降は
     * 緊急通知の状態画面に、消えない注意として常に出し続ける。
     */
    fun markWarningShown(context: Context) {
        prefs(context).edit().putBoolean(KEY_WARNING_SHOWN, true).apply()
    }

    /**
     * メーカー独自の「自動起動」設定画面。
     *
     * コンポーネント名は非公開で ROM のバージョンによって変わるため、実在を確認できたものだけを使い、
     * どれも見つからなければアプリの詳細設定にフォールバックする（開けずに落ちることは無い）。
     */
    fun autoStartIntent(context: Context): Intent {
        val candidates = when (current) {
            RestrictiveOs.HYPER_OS, RestrictiveOs.MIUI -> listOf(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity",
                ),
            )

            RestrictiveOs.COLOR_OS -> listOf(
                ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity",
                ),
                ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.startupapp.StartupAppListActivity",
                ),
                ComponentName(
                    "com.oplus.safecenter",
                    "com.oplus.safecenter.permission.startup.StartupAppListActivity",
                ),
                ComponentName(
                    "com.oppo.safe",
                    "com.oppo.safe.permission.startup.StartupAppListActivity",
                ),
            )

            null -> emptyList()
        }

        val packageManager = context.packageManager
        val resolved = candidates.firstOrNull { component ->
            runCatching { packageManager.getActivityInfo(component, 0) }.isSuccess
        }
        return if (resolved != null) {
            Intent().setComponent(resolved)
        } else {
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}"),
            )
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * システムプロパティを読む。`android.os.SystemProperties` は非公開 API で塞がれているため、
     * getprop コマンドの出力を読む。読めなければ空文字（＝検出しない側に倒す）。
     */
    private fun systemProperty(key: String): String = runCatching {
        val process = Runtime.getRuntime().exec(arrayOf("getprop", key))
        try {
            process.inputStream.bufferedReader().use { it.readLine() }?.trim().orEmpty()
        } finally {
            process.destroy()
        }
    }.getOrDefault("")
}
