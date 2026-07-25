package app.submissionultra.data

/**
 * 提出物の種類。手入力のみで、Classroom は自動連携しない（別アプリで見る前提）。
 */
enum class AssignmentType {
    CLASSROOM,
    PAPER,
}

/**
 * 種類ごとの「期限」欄の呼び方。
 * Classroom はオンライン提出なので「提出期限」。
 * 紙は物理的に手渡すため、その時刻までに終わらせておく必要がある「終わらせる時刻」。
 */
fun AssignmentType.deadlineLabel(): String = when (this) {
    AssignmentType.CLASSROOM -> "提出期限"
    AssignmentType.PAPER -> "終わらせる時刻"
}
