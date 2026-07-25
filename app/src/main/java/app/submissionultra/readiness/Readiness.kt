package app.submissionultra.readiness

/** 緊急通知の成立に関わる確認項目。 */
enum class ReadinessKey {
    NOTIFICATIONS,
    EXACT_ALARM,
    DND_ACCESS,
    EMERGENCY_CHANNEL,
    BATTERY_OPTIMIZATION,
    OVERLAY,
}

data class ReadinessItem(
    val key: ReadinessKey,
    val satisfied: Boolean,
    /** true なら緊急通知の成立に必須。false は強く推奨（バッテリー最適化）。 */
    val required: Boolean,
    val label: String,
    val detail: String,
)

data class ReadinessReport(val items: List<ReadinessItem>) {
    /** 必須項目がすべて満たされているか。false の間は「緊急通知できない」ことを明示する。 */
    val canFireEmergency: Boolean = items.filter { it.required }.all { it.satisfied }

    /** 未充足の項目（必須・推奨とも）。 */
    val unmet: List<ReadinessItem> = items.filter { !it.satisfied }
}
