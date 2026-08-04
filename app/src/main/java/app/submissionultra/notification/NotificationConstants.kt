package app.submissionultra.notification

object NotificationConstants {
    /**
     * 緊急通知チャンネル ID のプレフィックス。実際の ID は "emergency_v1", "emergency_v2" のように
     * 末尾にバージョン番号が付く（[EmergencyNotifier.currentChannelId]）。
     *
     * DND バイパスは「DND アクセスを持った状態でチャンネルを新規作成した時」にしか効かず、
     * 同じ ID の削除→再作成では古い設定が復活してしまう。そのため、アクセス取得後に
     * 新しいバージョン ID で作り直すことでバイパスを確実に有効化する。
     */
    const val EMERGENCY_CHANNEL_ID_PREFIX = "emergency_v"
    const val EMERGENCY_CHANNEL_NAME = "緊急通知"

    /**
     * リマインダー通知チャンネル ID。緊急通知とは別レベル。
     * DND はバイパスせず、重要度は DEFAULT（通常の通知音・マナーモードを尊重する）。
     */
    const val REMINDER_CHANNEL_ID = "reminder_v1"
    const val REMINDER_CHANNEL_NAME = "リマインダー通知"

    /** AlarmReceiver / ReminderReceiver / EmergencyActionReceiver に渡す提出物 ID。 */
    const val EXTRA_ASSIGNMENT_ID = "assignment_id"

    /** 通知アクション「完了にする」。EmergencyActionReceiver が受ける。 */
    const val ACTION_COMPLETE = "app.submissionultra.action.COMPLETE"

    /** 全画面アラーム画面(EmergencyAlarmActivity)へ表示用に渡す extras。 */
    const val EXTRA_TITLE = "title"
    const val EXTRA_DEADLINE_MILLIS = "deadline_millis"
    const val EXTRA_TEACHER = "teacher"

    /**
     * テスト通知で使うダミー提出物の ID。
     *
     * extras の取り出しに失敗したときの既定値（-1）と重なると、空の Intent が
     * テスト通知を鳴らしてしまう。既定値として現れようのない値にしておく。
     */
    const val TEST_ASSIGNMENT_ID = Long.MIN_VALUE

    /**
     * 緊急通知を無視されたまま放置されないよう、鳴らし直すまでの間隔。
     *
     * 一度きりの発火だと、通知を消した／アラーム画面をホームキーで離れただけで無音に戻ってしまう。
     * 完了になるか期限を過ぎるまでは、この間隔で鳴らし直す。
     */
    const val EMERGENCY_RETRY_INTERVAL_MILLIS = 5 * 60 * 1000L

    /**
     * 「開く」で作業に入ったあと、再発火を抑える時間。
     *
     * [EMERGENCY_RETRY_INTERVAL_MILLIS] より必ず長くする。同じ長さだと、抑制が切れる瞬間と
     * 再発火の瞬間が重なり、アプリで作業している最中に全画面へ叩き出される。
     */
    const val EMERGENCY_ACK_WINDOW_MILLIS = EMERGENCY_RETRY_INTERVAL_MILLIS + 60 * 1000L

    /**
     * アラームの生存確認（見張り）の間隔。
     *
     * 緊急アラームの再発火は「前回の発火に成功したレシーバ」の中でしか予約されない。
     * プロセスが落ちる・OS に配信を落とされるなどで一度でも鎖が切れると、次にアプリを
     * 手で開くまで永久に無音になる。それを避けるため、鎖とは独立にこの間隔で全体を引き直す。
     */
    const val WATCHDOG_INTERVAL_MILLIS = 6 * 60 * 60 * 1000L

    /**
     * 見張りが動いていれば、再設定の間隔は [WATCHDOG_INTERVAL_MILLIS] に収まる。
     * これを大きく超えていたら、その間アプリは止められていた（強制停止・OEM の省電力）。
     */
    const val ALARM_STALE_THRESHOLD_MILLIS = WATCHDOG_INTERVAL_MILLIS * 4

    /** 見張りアラームの PendingIntent 要求コード。提出物 ID とは衝突しない負値。 */
    const val WATCHDOG_REQUEST_CODE = -424242

    /**
     * 通知領域の目覚ましアイコンから開くための PendingIntent 要求コード。
     * 通知本文タップ用（要求コード＝提出物 ID）と重なると、後から作った方に extras を
     * 上書きされてしまうため、提出物 ID になり得ない値にしておく。
     */
    const val ALARM_SHOW_REQUEST_CODE = -424243

    /** 1 課題あたりのリマインダー枠数（3日前・2日前・1日前・当日 の最大 4 枠）。 */
    const val REMINDER_SLOT_COUNT = 4

    /** リマインダー通知の ID を緊急通知の ID と衝突させないためのオフセット。 */
    const val REMINDER_NOTIFICATION_OFFSET = 1_000_000L
}
