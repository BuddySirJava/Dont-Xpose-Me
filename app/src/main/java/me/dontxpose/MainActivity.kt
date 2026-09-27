// SPDX-License-Identifier: Apache-2.0

package me.dontxpose

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import me.dontxpose.ui.SetupScreen

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DontXposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    SetupScreen()
                }
            }
        }
    }
}

private val Danger = Color(0xFFE53935)
private val DangerMuted = Color(0xFF4A1515)
private val Safe = Color(0xFF66BB6A)
private val Ink = Color(0xFF0E0E10)
private val Panel = Color(0xFF18181C)
private val PanelHigh = Color(0xFF222228)

@Composable
fun DontXposeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Danger,
            onPrimary = Color.White,
            primaryContainer = DangerMuted,
            onPrimaryContainer = Color(0xFFFFCDD2),
            secondary = Safe,
            onSecondary = Color.Black,
            secondaryContainer = Color(0xFF1B3A1F),
            onSecondaryContainer = Color(0xFFC8E6C9),
            tertiary = Color(0xFF90CAF9),
            background = Ink,
            onBackground = Color(0xFFE8E8EA),
            surface = Panel,
            onSurface = Color(0xFFE8E8EA),
            surfaceVariant = PanelHigh,
            onSurfaceVariant = Color(0xFFB0B0B8),
            error = Danger,
            onError = Color.White,
            errorContainer = DangerMuted,
            onErrorContainer = Color(0xFFFFCDD2),
            outline = Color(0xFF3A3A42),
        ),
        typography = Typography(
            titleLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                letterSpacing = (-0.25).sp,
            ),
            titleMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
            ),
            bodyLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            ),
            bodyMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
            bodySmall = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Color(0xFFB0B0B8),
            ),
            labelLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            ),
        ),
        content = content,
    )
}
