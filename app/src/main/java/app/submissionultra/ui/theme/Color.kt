package app.submissionultra.ui.theme

import androidx.compose.ui.graphics.Color

// 配色は「見やすさ最優先」で組む。
// 面の階層（背景 < カード）をはっきり分け、文字は十分なコントラストを確保する。

// --- ライト ---
// 背景をわずかに色味のあるグレーにし、カード(白)が浮いて見えるようにする。
val BackgroundLight = Color(0xFFF3F5F8)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFE8ECF2)
val OnSurfaceLight = Color(0xFF141820)        // 本文：ほぼ黒で最大コントラスト
val OnSurfaceVariantLight = Color(0xFF515C69) // 補助文：白背景で十分な濃さ
val OutlineLight = Color(0xFFC2CAD4)
val OutlineVariantLight = Color(0xFFDDE3EA)

// --- ダーク ---
val BackgroundDark = Color(0xFF0E1116)
val SurfaceDark = Color(0xFF171C23)
val SurfaceVariantDark = Color(0xFF232B35)
val OnSurfaceDark = Color(0xFFE8ECF2)
val OnSurfaceVariantDark = Color(0xFFA9B4C2)
val OutlineDark = Color(0xFF3A4450)
val OutlineVariantDark = Color(0xFF2A323C)

// --- 操作を表す色（ボタン・FAB・選択中のタブ）---
// 文字色と明確に違う色にして「押せる」ことが一目で分かるようにする。
val PrimaryLight = Color(0xFF1B5FCE)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFD9E4FB)
val OnPrimaryContainerLight = Color(0xFF0B2E69)

val PrimaryDark = Color(0xFF9CBEFF)
val OnPrimaryDark = Color(0xFF0A2A63)
val PrimaryContainerDark = Color(0xFF1C3A6E)
val OnPrimaryContainerDark = Color(0xFFD9E4FB)

// --- 状態を表す色（信号機と同じ並びで直感的に読める）---
// 余裕あり=緑 / 今すぐ着手=橙 / 期限超過=赤
val Ok = Color(0xFF17803D)
val OkDark = Color(0xFF56D97F)

val Attention = Color(0xFFB45309)
val AttentionDark = Color(0xFFFBBF24)

val Alert = Color(0xFFC62828)
val AlertDark = Color(0xFFFF7B72)
