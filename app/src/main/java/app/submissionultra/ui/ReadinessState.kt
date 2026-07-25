package app.submissionultra.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.submissionultra.notification.EmergencyNotifier
import app.submissionultra.readiness.ReadinessChecker
import app.submissionultra.readiness.ReadinessReport

/**
 * 現在の緊急通知の成立状態と、それを手動で再確認する手段。
 * [report] は最新の確認結果、[refresh] を呼ぶと今この瞬間の端末状態で確認し直す。
 */
class ReadinessHolder(
    val report: ReadinessReport,
    val refresh: () -> Unit,
)

/**
 * 緊急通知の成立状態を保持する。次のタイミングで必ず再確認される:
 * - アプリが前面に戻ったとき（ON_RESUME）— 端末設定から戻ってきた直後など
 * - [ReadinessHolder.refresh] を明示的に呼んだとき — 設定画面を開いたとき・更新ボタン
 *
 * 権限は信頼せず毎回確認する、という哲学をそのまま反映する。
 */
@Composable
fun rememberReadinessState(): ReadinessHolder {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val reportState = remember { mutableStateOf(checkNow(context)) }
    val refresh = remember { { reportState.value = checkNow(context) } }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                reportState.value = checkNow(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return ReadinessHolder(reportState.value, refresh)
}

/**
 * 確認直前にチャンネル設定を再適用してから、現在の端末状態を確認する。
 * 再適用により、DND アクセスを許可して戻ってきた瞬間にバイパスが有効化され、
 * 表示が実態と一致する（プロセス再起動を待たない）。
 */
private fun checkNow(context: Context): ReadinessReport {
    EmergencyNotifier.ensureChannel(context)
    return ReadinessChecker.check(context)
}
