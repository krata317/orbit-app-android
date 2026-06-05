package com.krata.orbit.ui.events

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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.krata.orbit.data.model.*
import com.krata.orbit.ui.components.*
import com.krata.orbit.ui.theme.HarmonyOsSansFamily
import com.krata.orbit.utils.DateUtils
import com.krata.orbit.utils.DateUtils.toLocalDateTime
import com.krata.orbit.viewmodel.EventsViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.roundToInt
import androidx.compose.material3.ExperimentalMaterial3Api

@Composable
fun EventsScreen(viewModel: EventsViewModel) {
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingEvent  by remember { mutableStateOf<Event?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item { TabBanner(title = "Events") }

            item {
                Row(
                    modifier              = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OrbitButton(
                        text     = if (uiState.isSelectionMode) "Cancel" else "Add Event",
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
                            text     = "Delete Events",
                            onClick  = { viewModel.enterSelectionMode() },
                            modifier = Modifier.weight(1f),
                            icon     = { Icon(Icons.Default.DeleteOutline, null, Modifier.size(18.dp)) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (uiState.events.isEmpty()) {
                item { EmptyState("No events yet. Tap 'Add Event' to begin.", modifier = Modifier.padding(16.dp)) }
            } else {
                items(uiState.events, key = { it.id }) { event ->
                    EventCard(
                        event           = event,
                        isSelectionMode = uiState.isSelectionMode,
                        isSelected      = event.id in uiState.selectedIds,
                        onToggle        = { viewModel.toggleEvent(event) },
                        onSelect        = { viewModel.toggleSelection(event.id) },
                        onEdit          = { editingEvent = event },
                        modifier        = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (showAddDialog || editingEvent != null) {
            EventDialog(
                existing  = editingEvent,
                onConfirm = { event ->
                    if (editingEvent != null) viewModel.updateEvent(event)
                    else viewModel.addEvent(event)
                    showAddDialog = false; editingEvent = null
                },
                onDismiss = { showAddDialog = false; editingEvent = null }
            )
        }
    }
}

// ── Event card ────────────────────────────────────────────────────────────────
@Composable
fun EventCard(
    event: Event,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 120f

    val bgColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    else MaterialTheme.colorScheme.surfaceContainer

    Box(
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
                imageVector        = Icons.Default.Event,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(22.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = event.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color          = if (event.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                                         else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (event.isCompleted) TextDecoration.LineThrough else null,
                        fontWeight     = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text  = DateUtils.formatDateTime(event.dateTimeMillis),
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text  = if (event.isRecurring) "Recurring · ${event.frequency.name.lowercase().replaceFirstChar { it.uppercase() }}"
                            else "One-time",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }

            if (isSelectionMode) {
                CircularCheckbox(checked = isSelected, onCheckedChange = { onSelect() })
            } else {
                CircularCheckbox(checked = event.isCompleted, onCheckedChange = { onToggle() })
            }
        }
    }
}

// ── Add/Edit event dialog ─────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDialog(
    existing: Event?,
    onConfirm: (Event) -> Unit,
    onDismiss: () -> Unit
) {
    val initDt = existing?.dateTimeMillis?.toLocalDateTime() ?: LocalDateTime.now().plusHours(1)

    var name        by remember { mutableStateOf(existing?.name ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var isRecurring by remember { mutableStateOf(existing?.isRecurring ?: false) }
    var frequency   by remember { mutableStateOf(existing?.frequency ?: EventFrequency.NONE) }
    var weekday     by remember { mutableIntStateOf(existing?.weekday ?: 1) }
    var monthDays   by remember { mutableStateOf(existing?.monthDays ?: emptyList<Int>()) }

    // Date/time pickers (simple number fields for simplicity; production would use DatePickerDialog)
    var dateYear    by remember { mutableIntStateOf(initDt.year) }
    var dateMonth   by remember { mutableIntStateOf(initDt.monthValue) }
    var dateDay     by remember { mutableIntStateOf(initDt.dayOfMonth) }
    var timeHour    by remember { mutableIntStateOf(initDt.hour) }
    var timeMinute  by remember { mutableIntStateOf(initDt.minute) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.of(dateYear, dateMonth, dateDay)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        val ld = java.time.Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
                        dateYear = ld.year; dateMonth = ld.monthValue; dateDay = ld.dayOfMonth
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = timeHour, initialMinute = timeMinute)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = { timeHour = timePickerState.hour; timeMinute = timePickerState.minute; showTimePicker = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = timePickerState) }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Text(
                if (existing == null) "Add Event" else "Edit Event",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier              = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement   = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Event name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Description (optional)") }, maxLines = 3,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)
                )

                // Date picker row
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.CalendarToday, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("%02d/%02d/%04d".format(dateDay, dateMonth, dateYear))
                }

                // Time picker row
                OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.Schedule, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("%02d:%02d".format(timeHour, timeMinute))
                }

                // Recurring toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Recurring", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isRecurring, onCheckedChange = {
                        isRecurring = it
                        if (!it) frequency = EventFrequency.NONE
                    })
                }

                if (isRecurring) {
                    // Frequency selector
                    Text("Frequency", style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(EventFrequency.DAILY, EventFrequency.WEEKLY, EventFrequency.MONTHLY).forEach { freq ->
                            FilterChip(
                                selected = frequency == freq,
                                onClick  = { frequency = freq },
                                label    = { Text(freq.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }

                    if (frequency == EventFrequency.WEEKLY) {
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

                    if (frequency == EventFrequency.MONTHLY) {
                        Text("Days of month", style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        val allDays = (1..31).toList()
                        Column {
                            for (row in allDays.chunked(7)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    row.forEach { d ->
                                        FilterChip(
                                            selected = d in monthDays,
                                            onClick  = {
                                                monthDays = if (d in monthDays) monthDays - d else monthDays + d
                                            },
                                            label    = { Text("$d", style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
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
                    val dt = LocalDateTime.of(dateYear, dateMonth, dateDay, timeHour, timeMinute)
                    val ms = dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    onConfirm(
                        Event(
                            id            = existing?.id ?: 0,
                            name          = name.trim(),
                            description   = description.trim(),
                            dateTimeMillis = ms,
                            isRecurring   = isRecurring,
                            frequency     = if (isRecurring) frequency else EventFrequency.NONE,
                            weekday       = weekday,
                            monthDays     = monthDays
                        )
                    )
                }
            }) { Text(if (existing == null) "Add" else "Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Discard") } }
    )
}
