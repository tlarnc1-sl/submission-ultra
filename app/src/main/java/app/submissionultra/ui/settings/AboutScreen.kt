package app.submissionultra.ui.settings

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import app.submissionultra.ui.components.BackTopBar
import app.submissionultra.ui.formatDate

/** アプリ自身の情報。ハードコードせず、インストールされている実物から読み取る。 */
internal data class AppInfo(
    val icon: ImageBitmap,
    val name: String,
    val versionName: String,
    val versionCode: Long,
    val lastUpdatedMillis: Long,
)

private const val ICON_PX = 144

/** 配布しているライセンス。リポジトリ直下の LICENSE と一致させること。 */
private const val LICENSE_NAME = "MIT License"

/** 公開リポジトリ。ここから誰でもソースコードを読める。 */
private const val REPOSITORY_URL = "https://github.com/tlarnc1-sl/submission-ultra"

@Composable
internal fun rememberAppInfo(): AppInfo? {
    val context = LocalContext.current
    return remember {
        val pm = context.packageManager
        runCatching {
            val packageInfo = pm.getPackageInfo(context.packageName, 0)
            AppInfo(
                icon = pm.getApplicationIcon(context.packageName)
                    .toBitmap(width = ICON_PX, height = ICON_PX)
                    .asImageBitmap(),
                name = context.applicationInfo.loadLabel(pm).toString(),
                versionName = packageInfo.versionName ?: "-",
                versionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
                lastUpdatedMillis = packageInfo.lastUpdateTime,
            )
        }.getOrNull()
    }
}

/** このアプリについて：アイコン・アプリ名・バージョン・最終更新日だけを示す画面。 */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val info = rememberAppInfo()

    Scaffold(
        topBar = { BackTopBar("このアプリについて", onBack) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (info == null) {
                Text(
                    "アプリ情報を取得できませんでした",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    bitmap = info.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(20.dp)),
                )
                Text(info.name, style = MaterialTheme.typography.titleLarge)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            InfoRow("バージョン", "${info.versionName} (${info.versionCode})")
            InfoRow("最終更新日", formatDate(info.lastUpdatedMillis))
            InfoRow("ライセンス", LICENSE_NAME)

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, REPOSITORY_URL.toUri()),
                            )
                        }
                    }
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "ソースコード",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    REPOSITORY_URL,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Text(
                "このアプリはオープンソースです。誰でもソースコードを読み、自由に利用・改変できます。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
