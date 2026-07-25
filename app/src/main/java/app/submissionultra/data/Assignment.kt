package app.submissionultra.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 提出物。時刻はタイムゾーン依存を避けるため epoch ミリ秒（UTC 基準の絶対時刻）で保存する。
 * 「最後の開始時刻」は保存せず、余裕時間と併せて都度計算する（[app.submissionultra.domain.Timing]）。
 */
@Entity(tableName = "assignments")
data class Assignment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val type: AssignmentType,
    /** 期限（epoch ミリ秒）。 */
    val deadlineEpochMillis: Long,
    /** 本気でやれば終わる作業時間（分）。 */
    val effortMinutes: Int,
    val isCompleted: Boolean = false,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    /** 完了にした時刻（epoch ミリ秒）。未完了なら null。完了履歴の並び替えに使う。 */
    val completedAtEpochMillis: Long? = null,
    /** 管理をする先生の名前（任意）。未設定なら null。 */
    val teacherName: String? = null,
)
