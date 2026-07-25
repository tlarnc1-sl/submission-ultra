package app.submissionultra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 提出物の緊急度は色分けしない（読みやすさを優先し、大きさ・太さ・並び順で表す）。
// 色を使うのは「緊急通知が鳴るか鳴らないか」のように、見落とすと実害がある状態表示だけ。

/** 設定が満たされている状態を表す色（緑）。 */
@Composable
fun okColor(): Color = if (isSystemInDarkTheme()) OkDark else Ok
