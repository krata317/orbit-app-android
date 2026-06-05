package com.krata.orbit.ui.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krata.orbit.ui.theme.BelfastGroteskBlackFamily
import com.krata.orbit.ui.theme.HarmonyOsSansFamily

@Composable
fun WelcomeScreen(onGetStarted: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text      = "Welcome to\nOrbit",
                style     = MaterialTheme.typography.displaySmall.copy(
                    fontFamily   = BelfastGroteskBlackFamily,
                    fontWeight   = FontWeight.Black,
                    color        = MaterialTheme.colorScheme.onBackground,
                    fontSize     = 48.sp,
                    lineHeight   = 56.sp
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text      = "your life's operating system",
                style     = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = HarmonyOsSansFamily,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text  = "What should we call you?",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = HarmonyOsSansFamily,
                    color      = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            )

            OutlinedTextField(
                value         = name,
                onValueChange = { name = it },
                placeholder   = { Text("Your name", style = MaterialTheme.typography.bodyLarge) },
                singleLine    = true,
                shape         = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction      = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (name.isNotBlank()) onGetStarted(name.trim())
                    }
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Button(
                onClick  = { if (name.isNotBlank()) onGetStarted(name.trim()) },
                enabled  = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape    = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text  = "Get Started",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = HarmonyOsSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp
                    )
                )
            }
        }
    }
}
