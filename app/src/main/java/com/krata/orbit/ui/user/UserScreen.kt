package com.krata.orbit.ui.user

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.krata.orbit.BuildConfig
import com.krata.orbit.data.api.UpdateChecker
import com.krata.orbit.data.local.NotifPrefs
import com.krata.orbit.ui.components.GradientBanner
import com.krata.orbit.ui.components.OrbitButton
import com.krata.orbit.ui.components.TabBanner
import com.krata.orbit.ui.theme.AppTheme
import com.krata.orbit.ui.theme.BelfastGroteskBlackFamily
import com.krata.orbit.ui.theme.HarmonyOsSansFamily
import com.krata.orbit.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api

@Composable
fun UserScreen(
    appViewModel: AppViewModel,
    currentTheme: AppTheme
) {
    val appState    by appViewModel.state.collectAsStateWithLifecycle()
    val notifPrefs  by appViewModel.getNotifPrefsFlow().collectAsStateWithLifecycle(initialValue = NotifPrefs())
    val context     = LocalContext.current
    val scope       = rememberCoroutineScope()

    var showNameDialog    by remember { mutableStateOf(false) }
    var showThemeDialog   by remember { mutableStateOf(false) }
    var showNotifScreen   by remember { mutableStateOf(false) }
    var showResetDialog   by remember { mutableStateOf(false) }
    var updateMessage     by remember { mutableStateOf<String?>(null) }
    var isCheckingUpdate  by remember { mutableStateOf(false) }

    if (showNotifScreen) {
        NotificationSettingsScreen(
            current  = notifPrefs,
            onSave   = { appViewModel.applyNotifPrefs(it) },
            onBack   = { showNotifScreen = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        TabBanner(title = "Profile")

        // ── Profile card ───────────────────────────────────────────────────────
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            GradientBanner(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = appState.username.ifBlank { "User" },
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = BelfastGroteskBlackFamily,
                            fontWeight = FontWeight.Black,
                            color      = Color.White,
                            fontSize   = 40.sp
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Settings card ──────────────────────────────────────────────────────
        SettingsCard(
            title = "Settings",
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            SettingsRow(
                icon    = Icons.Default.Person,
                label   = "Change Username",
                onClick = { showNameDialog = true }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            SettingsRow(
                icon    = Icons.Default.Palette,
                label   = "Display Mode",
                value   = when (currentTheme) {
                    AppTheme.DARK   -> "Dark"
                    AppTheme.LIGHT  -> "Light"
                    AppTheme.SYSTEM -> "System"
                },
                onClick = { showThemeDialog = true }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            SettingsRow(
                icon    = Icons.Default.Notifications,
                label   = "Notifications",
                onClick = { showNotifScreen = true }
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── App card ───────────────────────────────────────────────────────────
        SettingsCard(
            title = "App",
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            SettingsRow(
                icon    = Icons.Default.SystemUpdate,
                label   = "Check for Updates",
                value   = if (isCheckingUpdate) "Checking…" else updateMessage,
                onClick = {
                    scope.launch {
                        isCheckingUpdate = true
                        updateMessage    = null
                        val info = UpdateChecker.checkForUpdate(BuildConfig.VERSION_NAME)
                        isCheckingUpdate = false
                        if (info == null) {
                            updateMessage = "Could not check"
                        } else if (info.hasUpdate) {
                            updateMessage = "v${info.latestVersion} available!"
                            // Open release page
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.releaseUrl))
                            context.startActivity(intent)
                        } else {
                            updateMessage = "Up to date ✓"
                        }
                    }
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            SettingsRow(
                icon    = Icons.Default.DeleteForever,
                label   = "Reset App",
                isDestructive = true,
                onClick = { showResetDialog = true }
            )
        }

        Spacer(Modifier.height(32.dp))

        // ── Footer ─────────────────────────────────────────────────────────────
        Column(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text      = "made with love by Krata",
                style     = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = HarmonyOsSansFamily,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text      = "Copyright 2026, all rights reserved",
                style     = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = HarmonyOsSansFamily,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(120.dp))
    }

    // ── Dialogs ────────────────────────────────────────────────────────────────
    if (showNameDialog) {
        ChangeUsernameDialog(
            current   = appState.username,
            onConfirm = { appViewModel.setUsername(it); showNameDialog = false },
            onDismiss = { showNameDialog = false }
        )
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            current   = currentTheme,
            onSelect  = { appViewModel.setTheme(it); showThemeDialog = false },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor   = MaterialTheme.colorScheme.surfaceContainer,
            title = { Text("Reset App", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text  = { Text("This will permanently delete ALL your data and restart the app from scratch. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    appViewModel.resetApp()
                    showResetDialog = false
                }) {
                    Text("Reset", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Cancel") } }
        )
    }
}

// ── Settings card container ───────────────────────────────────────────────────
@Composable
private fun SettingsCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text     = title.uppercase(),
            style    = MaterialTheme.typography.labelLarge.copy(
                color        = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.5.sp,
                fontWeight   = FontWeight.Bold
            ),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )
        Card(
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(content = content)
        }
    }
}

// ── Settings row ──────────────────────────────────────────────────────────────
@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    value: String? = null,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val contentColor = if (isDestructive) MaterialTheme.colorScheme.error
                       else MaterialTheme.colorScheme.onSurface

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge.copy(color = contentColor), modifier = Modifier.weight(1f))
        if (value != null) {
            Text(value, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

// ── Change username dialog ────────────────────────────────────────────────────
@Composable
private fun ChangeUsernameDialog(current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("Change Username", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("New username") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Discard") } }
    )
}

// ── Theme picker dialog ───────────────────────────────────────────────────────
@Composable
private fun ThemePickerDialog(current: AppTheme, onSelect: (AppTheme) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("Display Mode", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
        text = {
            Column {
                AppTheme.entries.forEach { theme ->
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(theme) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(selected = current == theme, onClick = { onSelect(theme) })
                        Text(
                            theme.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

// ── Notification settings screen ──────────────────────────────────────────────
@Composable
fun NotificationSettingsScreen(
    current: NotifPrefs,
    onSave: (NotifPrefs) -> Unit,
    onBack: () -> Unit
) {
    var tasksEnabled   by remember { mutableStateOf(current.tasksEnabled) }
    var tasksHour      by remember { mutableIntStateOf(current.tasksHour) }
    var tasksMinute    by remember { mutableIntStateOf(current.tasksMinute) }
    var potdEnabled    by remember { mutableStateOf(current.potdEnabled) }
    var potdHour       by remember { mutableIntStateOf(current.potdHour) }
    var potdMinute     by remember { mutableIntStateOf(current.potdMinute) }
    var eventsEnabled  by remember { mutableStateOf(current.eventsEnabled) }
    var eventsMinsBefore by remember { mutableIntStateOf(current.eventsMinutesBefore) }
    var contestsEnabled by remember { mutableStateOf(current.contestsEnabled) }
    var contestsMinsBefore by remember { mutableIntStateOf(current.contestsMinutesBefore) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 20.dp, top = 52.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text(
                "Notifications",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = BelfastGroteskBlackFamily, fontWeight = FontWeight.Black
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tasks
            NotifCategoryCard(
                title       = "Tasks",
                enabled     = tasksEnabled,
                onToggle    = { tasksEnabled = it }
            ) {
                if (tasksEnabled) {
                    TimeSelector("Notify at", tasksHour, tasksMinute, { tasksHour = it }, { tasksMinute = it })
                }
            }

            // POTD
            NotifCategoryCard(
                title    = "Problem of the Day",
                enabled  = potdEnabled,
                onToggle = { potdEnabled = it }
            ) {
                if (potdEnabled) {
                    TimeSelector("Notify at", potdHour, potdMinute, { potdHour = it }, { potdMinute = it })
                }
            }

            // Events
            NotifCategoryCard(
                title    = "Events",
                enabled  = eventsEnabled,
                onToggle = { eventsEnabled = it }
            ) {
                if (eventsEnabled) {
                    MinutesBeforeSelector("Minutes before event", eventsMinsBefore) { eventsMinsBefore = it }
                }
            }

            // Contests
            NotifCategoryCard(
                title    = "Contests",
                enabled  = contestsEnabled,
                onToggle = { contestsEnabled = it }
            ) {
                if (contestsEnabled) {
                    MinutesBeforeSelector("Minutes before contest", contestsMinsBefore) { contestsMinsBefore = it }
                }
            }

            // Save button
            OrbitButton(
                text = "Save", onClick = {
                    onSave(NotifPrefs(
                        tasksEnabled         = tasksEnabled,
                        tasksHour            = tasksHour,
                        tasksMinute          = tasksMinute,
                        potdEnabled          = potdEnabled,
                        potdHour             = potdHour,
                        potdMinute           = potdMinute,
                        eventsEnabled        = eventsEnabled,
                        eventsMinutesBefore  = eventsMinsBefore,
                        contestsEnabled      = contestsEnabled,
                        contestsMinutesBefore = contestsMinsBefore
                    ))
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun NotifCategoryCard(
    title: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSelector(
    label: String,
    hour: Int, minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { showPicker = true }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text("%02d:%02d".format(hour, minute), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
    }

    if (showPicker) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute)
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = { onHourChange(state.hour); onMinuteChange(state.minute); showPicker = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = state) }
        )
    }
}

@Composable
private fun MinutesBeforeSelector(label: String, minutes: Int, onChange: (Int) -> Unit) {
    val options = listOf(5, 10, 15, 30, 60, 120, 180)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { opt ->
                FilterChip(
                    selected = minutes == opt,
                    onClick  = { onChange(opt) },
                    label    = { Text(if (opt < 60) "${opt}m" else "${opt/60}h", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}
