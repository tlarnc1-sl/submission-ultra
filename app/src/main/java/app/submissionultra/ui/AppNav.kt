package app.submissionultra.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.submissionultra.readiness.OemPowerSettings
import app.submissionultra.ui.components.OemPowerWarningDialog
import app.submissionultra.ui.completed.CompletedScreen
import app.submissionultra.ui.edit.EditScreen
import app.submissionultra.ui.home.HomeScreen
import app.submissionultra.ui.settings.AboutScreen
import app.submissionultra.ui.settings.ReadinessScreen
import app.submissionultra.ui.settings.SettingsScreen
import app.submissionultra.ui.settings.TestNotificationScreen
import app.submissionultra.ui.settings.TimeSettingsScreen

object Routes {
    const val HOME = "home"
    const val COMPLETED = "completed"
    const val SETTINGS = "settings"
    const val SETTINGS_TIME = "settings/time"
    const val SETTINGS_TEST = "settings/test"
    const val SETTINGS_STATUS = "settings/status"
    const val SETTINGS_ABOUT = "settings/about"
    const val EDIT_WITH_ARG = "edit?id={id}"
    fun edit(id: Long) = "edit?id=$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "ホーム", Icons.Default.Home),
    Tab(Routes.COMPLETED, "完了済み", Icons.Default.CheckCircle),
    Tab(Routes.SETTINGS, "設定", Icons.Default.Settings),
)

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val navController = rememberNavController()
    // 前面に戻るたびに再確認され、全画面が同じ結果を共有する。手動更新も可能。
    val readiness = rememberReadinessState()

    // 独自の省電力機能を持つ OS では、起動時チェックが全項目 OK でも通知が遅れうる。
    // その穴だけはアプリから検知できないので、初回起動時に一度だけ正面から知らせる。
    var oemWarningOs by remember {
        mutableStateOf(if (OemPowerSettings.shouldWarn(context)) OemPowerSettings.current else null)
    }
    oemWarningOs?.let { os ->
        OemPowerWarningDialog(
            osName = os.displayName,
            onOpenSettings = {
                OemPowerSettings.markWarningShown(context)
                oemWarningOs = null
                runCatching { context.startActivity(OemPowerSettings.autoStartIntent(context)) }
            },
            onLater = {
                // 割り込むのはここまで。以降は「緊急通知の状態」に消えない注意として残る。
                OemPowerSettings.markWarningShown(context)
                oemWarningOs = null
            },
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val messenger = remember(snackbarHostState, scope) { AppMessenger(snackbarHostState, scope) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // 編集中はナビを隠して作業に集中させる。
    val showBottomBar = currentRoute != Routes.EDIT_WITH_ARG

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = backStackEntry?.destination?.hierarchy
                            ?.any { it.route == tab.route } == true ||
                            (tab.route == Routes.SETTINGS && currentRoute?.startsWith("settings") == true)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            // 下のナビゲーション分だけを空ける。上のステータスバー分は各画面の
            // TopAppBar が自分で確保するので、ここで足すとタイトル上が二重に空いてしまう。
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    readiness = readiness.report,
                    onAddAssignment = { navController.navigate(Routes.edit(0L)) },
                    onEditAssignment = { id -> navController.navigate(Routes.edit(id)) },
                    onOpenSettings = { navController.switchTab(Routes.SETTINGS) },
                    messenger = messenger,
                )
            }
            composable(
                route = Routes.EDIT_WITH_ARG,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                EditScreen(
                    assignmentId = id,
                    onDone = { navController.popBackStack() },
                    messenger = messenger,
                )
            }
            composable(Routes.COMPLETED) {
                CompletedScreen(messenger = messenger)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    readiness = readiness.report,
                    onOpenTime = { navController.navigate(Routes.SETTINGS_TIME) },
                    onOpenTest = { navController.navigate(Routes.SETTINGS_TEST) },
                    onOpenStatus = { navController.navigate(Routes.SETTINGS_STATUS) },
                    onOpenAbout = { navController.navigate(Routes.SETTINGS_ABOUT) },
                )
            }
            composable(Routes.SETTINGS_ABOUT) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_TIME) {
                TimeSettingsScreen(
                    onBack = { navController.popBackStack() },
                    messenger = messenger,
                )
            }
            composable(Routes.SETTINGS_TEST) {
                TestNotificationScreen(
                    onBack = { navController.popBackStack() },
                    messenger = messenger,
                )
            }
            composable(Routes.SETTINGS_STATUS) {
                ReadinessScreen(
                    readiness = readiness.report,
                    onRefresh = readiness.refresh,
                    onBack = { navController.popBackStack() },
                    messenger = messenger,
                )
            }
        }
    }
}

/** タブ切替：積み上げず、戻ったときは元のスクロール位置を復元する。 */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
