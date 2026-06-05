package com.krata.orbit.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krata.orbit.ui.theme.BelfastGroteskBlackFamily
import com.krata.orbit.ui.theme.HarmonyOsSansFamily
import com.krata.orbit.ui.theme.RingiftFamily
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(700, easing = EaseOut))
        delay(1200)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-60).dp)
                .alpha(alpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Orbit",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily   = BelfastGroteskBlackFamily,
                    fontWeight   = FontWeight.Black,
                    color        = MaterialTheme.colorScheme.onBackground,
                    fontSize     = 50.sp,
                    letterSpacing = 4.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text      = "A Student's Personal Manager",
                style     = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = HarmonyOsSansFamily,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize   = 15.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
