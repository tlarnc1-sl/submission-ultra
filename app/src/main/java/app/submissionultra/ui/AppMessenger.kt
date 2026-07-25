package app.submissionultra.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * アプリ全体で共通のフィードバック窓口。操作が確かに反映されたことを必ず伝え、
 * 取り消せる操作には「取消」を添える。
 */
@Stable
class AppMessenger(
    private val hostState: SnackbarHostState,
    private val scope: CoroutineScope,
) {
    /** 単純な通知。 */
    fun show(message: String) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            hostState.showSnackbar(message)
        }
    }

    /** 取り消せる操作。ユーザーが「取消」を押したら [onUndo] を実行する。 */
    fun showUndo(message: String, actionLabel: String = "取消", onUndo: () -> Unit) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            val result = hostState.showSnackbar(message = message, actionLabel = actionLabel)
            if (result == SnackbarResult.ActionPerformed) onUndo()
        }
    }
}
