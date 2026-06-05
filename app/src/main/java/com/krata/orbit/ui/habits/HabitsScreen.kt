package com.krata.orbit.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.krata.orbit.data.model.*
import com.krata.orbit.ui.components.*
import com.krata.orbit.ui.theme.BelfastGroteskBlackFamily
import com.krata.orbit.ui.theme.HarmonyOsSansFamily
import com.krata.orbit.viewmodel.HabitsViewModel
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

@Composable
fun HabitsScreen(viewModel: HabitsViewModel) {
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingHabit  by remember { mutableStateOf<Habit?>(null) }
    var detailHabit   by remember { mutableStateOf<Habit?>(null) }

    // Show detail screen if one is selected
    detailHabit?.let { habit ->
        HabitDetailScreen(
            habitId   = habit.id,
            viewModel = viewModel,
            onBack    = { detailHabit = null }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item { TabBanner(title = "Habits") }

            item {
                Row(
                    modifier              = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OrbitButton(
                        text     = if (uiState.isSelectionMode) "Cancel" else "Add Habit",
                        onClick  = { if (uiState.isSelectionMode) viewModel.exitSelectionMode() else showAddDialog = true },
                        modifier = Modifier.weight(1f),
                        icon     = if (!uiState.isSelectionMode) ({ Icon(Icons.Default.Add, null, Modifier.size(18.dp)) }) else null
                    )
                    if (uiState.isSelectionMode) {
                        OrbitButton(
                            text          = "Delete (${uiState.selectedIds.size})",
                            onClick       = { viewModel.deleteSelected() },
                            modifier      = Modifier.weight(1f),
                            isDestructive = true,
                            icon          = { Icon(Icons.Default.Delete, null, Modifier.size(18.dp)) }
                        )
                    } else {
                        OrbitOutlinedButton(
                            text     = "Delete Habits",
                            onClick  = { viewModel.enterSelectionMode() },
                            modifier = Modifier.weight(1f),
                            icon     = { Icon(Icons.Default.DeleteOutline, null, Modifier.size(18.dp)) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (uiState.habits.isEmpty()) {
                item { EmptyState("No habits yet. Tap 'Add Habit' to begin.", modifier = Modifier.padding(16.dp)) }
            } else {
                items(uiState.habits, key = { it.id }) { habit ->
                    HabitCard(
                        habit           = habit,
                        isSelectionMode = uiState.isSelectionMode,
                        isSelected      = habit.id in uiState.selectedIds,
                        onToggle        = { viewModel.toggleHabit(habit) },
                        onSelect        = { viewModel.toggleSelection(habit.id) },
                        onEdit          = { editingHabit = habit },
                        onMoreInfo      = { detailHabit = habit },
                        modifier        = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (showAddDialog || editingHabit != null) {
            HabitDialog(
                existing  = editingHabit,
                onConfirm = { habit ->
                    if (editingHabit != null) viewModel.updateHabit(habit)
                    else viewModel.addHabit(habit)
                    showAddDialog = false; editingHabit = null
                },
                onDismiss = { showAddDialog = false; editingHabit = null }
            )
        }
    }
}

// ── Habit detail screen ───────────────────────────────────────────────────────
@Composable
fun HabitDetailScreen(
    habitId: Long,
    viewModel: HabitsViewModel,
    onBack: () -> Unit
) {
    val detailFlow: Flow<com.krata.orbit.viewmodel.HabitDetailState?> =
        remember(habitId) { viewModel.getHabitDetail(habitId) }
    val detail by detailFlow.collectAsStateWithLifecycle(initialValue = null)

    detail?.let { state ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Banner with back button
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(start = 8.dp, end = 20.dp, top = 52.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text  = state.habit.name,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = BelfastGroteskBlackFamily,
                        fontWeight = FontWeight.Black,
                        color      = MaterialTheme.colorScheme.onBackground
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Streak stats card
                item {
                    OrbitCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatColumn(label = "Current Streak", value = "${state.habit.currentStreak} days")
                            Divider(modifier = Modifier.height(48.dp).width(1.dp), color = MaterialTheme.colorScheme.outline)
                            StatColumn(label = "Max Streak", value = "${state.habit.maxStreak} days")
                        }
                    }
                }

                // Achievements heading
                item {
                    SectionHeading(text = "ACHIEVEMENTS", modifier = Modifier.padding(top = 8.dp))
                }

                // Achievement cards
                items(HABIT_ACHIEVEMENT_DAYS) { days ->
                    val unlocked = days in state.unlockedAchievements
                    AchievementCard(days = days, unlocked = unlocked)
                }
            }
        }
    } ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text  = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = BelfastGroteskBlackFamily,
                fontWeight = FontWeight.Black,
                color      = MaterialTheme.colorScheme.onSurface
            )
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun AchievementCard(days: Int, unlocked: Boolean) {
    OrbitCard {
        Row(
            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint               = if (unlocked) Color(0xFFFFD700) else Color.Transparent,
                modifier           = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "$days Day${if (days > 1) "s" else ""} Streak",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (unlocked) FontWeight.Bold else FontWeight.Normal,
                        color      = if (unlocked) MaterialTheme.colorScheme.onSurface
                                     else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Text(
                    text  = if (unlocked) "Unlocked ✓" else "Locked",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (unlocked) Color(0xFF6BCB77) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

// ── Habit card ────────────────────────────────────────────────────────────────
@Composable
fun HabitCard(
    habit: Habit,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onMoreInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 120f

    val bgColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    else MaterialTheme.colorScheme.surfaceContainer

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            offsetX > swipeThreshold  -> { onToggle(); offsetX = 0f }
                            offsetX < -swipeThreshold -> { onEdit();   offsetX = 0f }
                            else                      -> { offsetX = 0f }
                        }
                    },
                    onHorizontalDrag = { _, delta ->
                        offsetX = (offsetX + delta).coerceIn(-200f, 200f)
                    }
                )
            }
            .clickable { if (isSelectionMode) onSelect() }
    ) {
        Row(
            modifier          = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(22.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = habit.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color          = if (habit.isCompletedToday) MaterialTheme.colorScheme.onSurfaceVariant
                                         else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (habit.isCompletedToday) TextDecoration.LineThrough else null,
                        fontWeight     = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text  = "${habit.currentStreak} day${if (habit.currentStreak != 1) "s" else ""} streak 🔥",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }

            if (isSelectionMode) {
                CircularCheckbox(checked = isSelected, onCheckedChange = { onSelect() })
            } else {
                CircularCheckbox(checked = habit.isCompletedToday, onCheckedChange = { onToggle() })
            }
        }

        // More info row
        if (!isSelectionMode) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMoreInfo() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text  = "More info",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = HarmonyOsSansFamily
                    )
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ExpandMore, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Add/Edit habit dialog ─────────────────────────────────────────────────────
@Composable
fun HabitDialog(
    existing: Habit?,
    onConfirm: (Habit) -> Unit,
    onDismiss: () -> Unit
) {
    var name      by remember { mutableStateOf(existing?.name ?: "") }
    var frequency by remember { mutableStateOf(existing?.frequency ?: HabitFrequency.DAILY) }
    var weekday   by remember { mutableIntStateOf(existing?.weekday ?: 1) }
    var monthDays by remember { mutableStateOf(existing?.monthDays ?: emptyList<Int>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Text(
                if (existing == null) "Add Habit" else "Edit Habit",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier            = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Habit name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)
                )

                Text("Frequency", style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HabitFrequency.entries.forEach { freq ->
                        FilterChip(
                            selected = frequency == freq,
                            onClick  = { frequency = freq },
                            label    = { Text(freq.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                if (frequency == HabitFrequency.WEEKLY) {
                    Text("Day of week", style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    val days = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        days.forEachIndexed { i, day ->
                            FilterChip(
                                selected = weekday == i + 1,
                                onClick  = { weekday = i + 1 },
                                label    = { Text(day, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                if (frequency == HabitFrequency.MONTHLY) {
                    Text("Days of month", style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Column {
                        (1..31).toList().chunked(7).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                row.forEach { d ->
                                    FilterChip(
                                        selected = d in monthDays,
                                        onClick  = { monthDays = if (d in monthDays) monthDays - d else monthDays + d },
                                        label    = { Text("$d", style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onConfirm(
                        Habit(
                            id            = existing?.id ?: 0,
                            name          = name.trim(),
                            frequency     = frequency,
                            weekday       = weekday,
                            monthDays     = monthDays,
                            currentStreak = existing?.currentStreak ?: 0,
                            maxStreak     = existing?.maxStreak ?: 0,
                            isCompletedToday  = existing?.isCompletedToday ?: false,
                            lastCompletedDate = existing?.lastCompletedDate ?: ""
                        )
                    )
                }
            }) { Text(if (existing == null) "Add" else "Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Discard") } }
    )
}
