package app.submissionultra.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.ui.platform.LocalContext
import app.submissionultra.appGraph
import app.submissionultra.data.Assignment
import app.submissionultra.readiness.ReadinessReport
import app.submissionultra.ui.AppMessenger
import app.submissionultra.ui.components.AssignmentCard
import app.submissionultra.ui.components.HeroCard
import app.submissionultra.ui.components.ReadinessBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    readiness: ReadinessReport,
    onAddAssignment: () -> Unit,
    onEditAssignment: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    messenger: AppMessenger,
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                HomeViewModel(context.appGraph.assignmentRepository, context.appGraph.settingsRepository)
            }
        },
    )
    val items by viewModel.items.collectAsStateWithLifecycle()

    val complete: (Assignment) -> Unit = { assignment ->
        viewModel.markCompleted(assignment)
        messenger.showUndo("「${assignment.title}」を完了にしました") {
            viewModel.undoComplete(assignment)
        }
    }

    // 最も切迫した1件はヒーローに出し、下のリストからは除く（重複させない）。
    val hero = items.firstOrNull()
    val rest = if (items.isEmpty()) emptyList() else items.drop(1)

    Scaffold(
        topBar = { TopAppBar(title = { Text("提出物Ultra") }) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddAssignment,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("追加") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            if (readiness.unmet.isNotEmpty()) {
                item(key = "readiness") {
                    ReadinessBanner(report = readiness, onOpenSettings = onOpenSettings)
                }
            }

            if (hero == null) {
                item(key = "empty") { EmptyState() }
            } else {
                item(key = "hero-${hero.assignment.id}") {
                    HeroCard(
                        item = hero,
                        onOpen = { onEditAssignment(hero.assignment.id) },
                        onComplete = { complete(hero.assignment) },
                    )
                }
            }

            if (rest.isNotEmpty()) {
                item(key = "header-rest") { SectionHeader("このあと") }
                items(rest, key = { it.assignment.id }) { homeItem ->
                    SwipeToComplete(onComplete = { complete(homeItem.assignment) }) {
                        AssignmentCard(
                            item = homeItem,
                            onClick = { onEditAssignment(homeItem.assignment.id) },
                        )
                    }
                }
            }
        }
    }
}

/** カードを横にスワイプして完了にする。取消は Snackbar から。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToComplete(
    onComplete: () -> Unit,
    content: @Composable () -> Unit,
) {
    // 一度のスワイプで二重に完了させないためのガード。
    var fired by remember { mutableStateOf(false) }
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled && !fired) {
                fired = true
                onComplete()
            }
            // 位置は戻す（完了によりリストから自然に消える）。
            false
        },
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        content = { content() },
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "未完了の提出物はありません",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "右下の「追加」から登録できます",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
