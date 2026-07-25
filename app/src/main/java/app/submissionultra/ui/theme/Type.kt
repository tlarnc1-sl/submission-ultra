package app.submissionultra.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.submissionultra.R

/**
 * 英数字は Google Sans Code、日本語は端末の標準フォント。
 *
 * このフォントは日本語のグリフを持たないため、指定しても平仮名・漢字は解決できず、
 * システムのフォントへ自動でフォールバックする。つまり「英数字だけ差し替える」ための
 * 指定はこの一つで足り、文字種ごとに使い分ける必要はない。
 *
 * 通信しないアプリなので、ダウンロード提供ではなく TTF を同梱している。
 */
private val GoogleSansCode = FontFamily(
    Font(R.font.google_sans_code_regular, FontWeight.Normal),
    Font(R.font.google_sans_code_medium, FontWeight.Medium),
    Font(R.font.google_sans_code_semibold, FontWeight.SemiBold),
    Font(R.font.google_sans_code_bold, FontWeight.Bold),
)

// ミニマルに徹しつつ、階層が一目で分かる一貫したタイポグラフィスケール。
// 装飾フォントは使わず、太さ・サイズ・字間だけで情報の優先度を表す。
val Typography = Typography(
    titleLarge = TextStyle(
        fontFamily = GoogleSansCode,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = GoogleSansCode,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = GoogleSansCode,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = GoogleSansCode,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.3.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = GoogleSansCode,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = GoogleSansCode,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = GoogleSansCode,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = GoogleSansCode,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
)
