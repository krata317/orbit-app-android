package com.krata.orbit.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.krata.orbit.data.model.Task
import com.krata.orbit.ui.components.*
import com.krata.orbit.ui.theme.*
import com.krata.orbit.viewmodel.HomeViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog    by remember { mutableStateOf(false) }
    var editingTask      by remember { mutableStateOf<Task?>(null) }

    val today      = remember { LocalDate.now() }
    val weekday    = today.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH))
    val dateStr    = today.format(DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH))

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // ── Banner ─────────────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(start = 20.dp, end = 20.dp, top = 56.dp, bottom = 4.dp)
                ) {
                    Text(
                        text  = "Welcome, ${uiState.tasks.let { _ -> "" }.let { "" }}",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = BelfastGroteskBlackFamily,
                            color      = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    // We'll get username from AppViewModel; for now the banner text is assembled in MainActivity
                    Text(
                        text  = "$weekday, $dateStr",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // ── Progress card ──────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ProgressCard(
                        total     = uiState.totalTasks,
                        completed = uiState.completedTasks
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Action buttons ────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OrbitButton(
                        text     = if (uiState.isSelectionMode) "Cancel" else "Add Task",
                        onClick  = {
                            if (uiState.isSelectionMode) viewModel.exitSelectionMode()
                            else showAddDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        icon     = if (!uiState.isSelectionMode) ({
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        }) else null
                    )
                    if (uiState.isSelectionMode) {
                        OrbitButton(
                            text          = "Delete (${uiState.selectedIds.size})",
                            onClick       = { viewModel.deleteSelected() },
                            modifier      = Modifier.weight(1f),
                            isDestructive = true,
                            icon          = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp)) }
                        )
                    } else {
                        OrbitOutlinedButton(
                            text    = "Delete Tasks",
                            onClick = { viewModel.enterSelectionMode() },
                            modifier = Modifier.weight(1f),
                            icon    = { Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Task list ──────────────────────────────────────────────────────
            if (uiState.tasks.isEmpty()) {
                item { EmptyState("No tasks yet. Tap 'Add Task' to begin.", modifier = Modifier.padding(16.dp)) }
            } else {
                items(uiState.tasks, key = { it.id }) { task ->
                    TaskCard(
                        task            = task,
                        isSelectionMode = uiState.isSelectionMode,
                        isSelected      = task.id in uiState.selectedIds,
                        onToggle        = { viewModel.toggleTask(task) },
                        onSelect        = { viewModel.toggleSelection(task.id) },
                        onEdit          = { editingTask = task },
                        modifier        = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Add / Edit dialog
        if (showAddDialog || editingTask != null) {
            TaskDialog(
                existing  = editingTask,
                onConfirm = { name, isUrgent, isRollover ->
                    if (editingTask != null) {
                        viewModel.updateTask(editingTask!!.copy(name = name, isUrgent = isUrgent, isRollover = isRollover))
                    } else {
                        viewModel.addTask(name, isUrgent, isRollover)
                    }
                    showAddDialog = false
                    editingTask   = null
                },
                onDismiss = { showAddDialog = false; editingTask = null }
            )
        }
    }
}

// ── HomeScreen needs username — it is passed down from MainActivity as a side-channel.
// We expose a version that accepts it directly:
@Composable
fun HomeScreen(viewModel: HomeViewModel, username: String) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTask   by remember { mutableStateOf<Task?>(null) }

    val today   = remember { LocalDate.now() }
    val weekday = today.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH))
    val dateStr = today.format(DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH))

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(start = 20.dp, end = 20.dp, top = 56.dp, bottom = 4.dp)
                ) {
                    Text(
                        text  = "Welcome, $username",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = BelfastGroteskBlackFamily,
                            fontWeight = FontWeight.Black,
                            color      = MaterialTheme.colorScheme.onBackground,
                            fontSize   = 28.sp
                        )
                    )
                    Text(
                        text  = "$weekday, $dateStr",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ProgressCard(total = uiState.totalTasks, completed = uiState.completedTasks)
                }
                Spacer(Modifier.height(12.dp))
            }

            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OrbitButton(
                        text    = if (uiState.isSelectionMode) "Cancel" else "Add Task",
                        onClick = { if (uiState.isSelectionMode) viewModel.exitSelectionMode() else showAddDialog = true },
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
                            text     = "Delete Tasks",
                            onClick  = { viewModel.enterSelectionMode() },
                            modifier = Modifier.weight(1f),
                            icon     = { Icon(Icons.Default.DeleteOutline, null, Modifier.size(18.dp)) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (uiState.tasks.isEmpty()) {
                item { EmptyState("No tasks yet. Tap 'Add Task' to begin.", modifier = Modifier.padding(16.dp)) }
            } else {
                items(uiState.tasks, key = { it.id }) { task ->
                    TaskCard(
                        task            = task,
                        isSelectionMode = uiState.isSelectionMode,
                        isSelected      = task.id in uiState.selectedIds,
                        onToggle        = { viewModel.toggleTask(task) },
                        onSelect        = { viewModel.toggleSelection(task.id) },
                        onEdit          = { editingTask = task },
                        modifier        = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (showAddDialog || editingTask != null) {
            TaskDialog(
                existing  = editingTask,
                onConfirm = { name, isUrgent, isRollover ->
                    if (editingTask != null) viewModel.updateTask(editingTask!!.copy(name = name, isUrgent = isUrgent, isRollover = isRollover))
                    else viewModel.addTask(name, isUrgent, isRollover)
                    showAddDialog = false; editingTask = null
                },
                onDismiss = { showAddDialog = false; editingTask = null }
            )
        }
    }
}

// ── Progress card ─────────────────────────────────────────────────────────────
@Composable
private fun ProgressCard(total: Int, completed: Int) {
    val percent = if (total == 0) -1f else completed.toFloat() / total.toFloat()

    OrbitCard {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle chart
            Box(
                modifier         = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                if (total == 0) {
                    Canvas(modifier = Modifier.size(72.dp)) {
                        drawArc(
                            color      = Color.White.copy(alpha = 0.1f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter  = false,
                            topLeft    = Offset(12f, 12f),
                            size       = Size(size.width - 24f, size.height - 24f),
                            style      = Stroke(width = 10f, cap = StrokeCap.Round)
                        )
                    }
                    Text("N/A", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                } else {
                    Canvas(modifier = Modifier.size(72.dp)) {
                        val stroke  = 10f
                        val inset   = Offset(12f, 12f)
                        val arcSize = Size(size.width - 24f, size.height - 24f)
                        drawArc(
                            color      = Color.White.copy(alpha = 0.1f),
                            startAngle = -90f, sweepAngle = 360f,
                            useCenter  = false, topLeft = inset, size = arcSize,
                            style      = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color      = SuccessColor,
                            startAngle = -90f,
                            sweepAngle = 360f * percent,
                            useCenter  = false, topLeft = inset, size = arcSize,
                            style      = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text  = "${(percent * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color      = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column {
                Text(
                    text  = "Today's Progress",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = HarmonyOsSansFamily,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(Modifier.height(4.dp))
                if (total == 0) {
                    Text("No tasks added yet", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                } else {
                    Text(
                        text  = "${(percent * 100).roundToInt()}% tasks complete today",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text  = "$completed / $total done",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }
    }
}

// ── Task card with swipe gestures ─────────────────────────────────────────────
@Composable
fun TaskCard(
    task: Task,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX    by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 120f

    val bgColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.surfaceContainer,
        tween(200), label = "taskbg"
    )

    Box(modifier = modifier
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
            // Task icon
            Icon(
                imageVector        = Icons.Default.CheckBoxOutlineBlank,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(22.dp)
            )

            // Name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text            = task.name,
                    style           = MaterialTheme.typography.bodyLarge.copy(
                        color          = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                                         else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                    ),
                    maxLines        = 2,
                    overflow        = TextOverflow.Ellipsis
                )
            }

            // Urgent badge
            if (task.isUrgent) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(UrgentColor.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("URGENT", style = MaterialTheme.typography.labelSmall.copy(color = UrgentColor, fontWeight = FontWeight.Bold))
                }
            }

            // Checkbox
            if (isSelectionMode) {
                CircularCheckbox(
                    checked        = isSelected,
                    onCheckedChange = { onSelect() }
                )
            } else {
                CircularCheckbox(
                    checked        = task.isCompleted,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }
}

// ── Add / Edit task dialog ────────────────────────────────────────────────────
@Composable
fun TaskDialog(
    existing: Task?,
    onConfirm: (name: String, isUrgent: Boolean, isRollover: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name      by remember { mutableStateOf(existing?.name ?: "") }
    var isUrgent  by remember { mutableStateOf(existing?.isUrgent ?: false) }
    var isRollover by remember { mutableStateOf(existing?.isRollover ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Text(
                text  = if (existing == null) "Add Task" else "Edit Task",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Task name") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(10.dp)
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Rollover to next day", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isRollover, onCheckedChange = { isRollover = it })
                }
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Urgent", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isUrgent, onCheckedChange = { isUrgent = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim(), isUrgent, isRollover) }) {
                Text(if (existing == null) "Add" else "Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Discard") }
        }
    )
}
