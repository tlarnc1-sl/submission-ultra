package app.submissionultra.notification

import app.submissionultra.data.Assignment
import app.submissionultra.data.AssignmentType

/**
 * テスト通知で使う共通のダミー提出物（緊急・リマインダー共通）。
 * 本番と違うのは「渡す提出物データがテスト用であること」だけ、を体現する唯一のダミー。
 */
fun testAssignment(): Assignment = Assignment(
    id = NotificationConstants.TEST_ASSIGNMENT_ID,
    title = "テスト通知",
    type = AssignmentType.PAPER,
    deadlineEpochMillis = System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000L,
    effortMinutes = 30,
    isCompleted = false,
)
