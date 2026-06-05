package com.krata.orbit.ui.budget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.krata.orbit.data.model.BudgetCategory
import com.krata.orbit.data.model.BudgetMonth
import com.krata.orbit.data.model.DEFAULT_BUDGET_CATEGORIES
import com.krata.orbit.data.model.Expense
import com.krata.orbit.ui.components.*
import com.krata.orbit.ui.theme.BelfastGroteskBlackFamily
import com.krata.orbit.ui.theme.HarmonyOsSansFamily
import com.krata.orbit.viewmodel.BudgetViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min
import androidx.compose.material3.ExperimentalMaterial3Api

@Composable
fun BudgetScreen(viewModel: BudgetViewModel) {
    val uiState     by viewModel.uiState.collectAsStateWithLifecycle()
    var showDetails  by remember { mutableStateOf(false) }
    var showAddExp   by remember { mutableStateOf(false) }
    var showModify   by remember { mutableStateOf(false) }
    var showExpLog   by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.ensureBudgetMonthExists() }

    if (showExpLog) {
        ExpenseLogScreen(
            expenses = uiState.expenses,
            onBack   = { showExpLog = false }
        )
        return
    }

    val ym = run {
        val parts = uiState.monthKey.split("-")
        if (parts.size == 2) YearMonth.of(parts[0].toInt(), parts[1].toInt())
        else YearMonth.now()
    }
    val monthLabel = ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item { TabBanner(title = "Budget") }

            // ── Month summary card ─────────────────────────────────────────────
            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    OrbitCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text  = monthLabel,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color      = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                BudgetStat(label = "Budget", value = "₹%.0f".format(uiState.budgetMonth?.budgetAmount ?: 0.0))
                                VerticalDivider(modifier = Modifier.height(40.dp))
                                BudgetStat(label = "Spent", value = "₹%.0f".format(uiState.totalExpenditure))
                                VerticalDivider(modifier = Modifier.height(40.dp))
                                BudgetStat(
                                    label     = "Left",
                                    value     = "₹%.0f".format(uiState.leftover),
                                    valueColor = if (uiState.leftover >= 0) Color(0xFF6BCB77) else Color(0xFFFF6B6B)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Daily bar chart card ───────────────────────────────────────────
            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    OrbitCard {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Daily Spending",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(Modifier.height(12.dp))
                            DailyBarChart(
                                expenseByDay  = uiState.expenseByDay,
                                daysInMonth   = ym.lengthOfMonth(),
                                modifier      = Modifier.fillMaxWidth().height(120.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Action buttons ─────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OrbitOutlinedButton(
                        text     = "Modify Details",
                        onClick  = { showModify = true },
                        modifier = Modifier.weight(1f),
                        icon     = { Icon(Icons.Default.Edit, null, Modifier.size(18.dp)) }
                    )
                    OrbitButton(
                        text     = "Add Expense",
                        onClick  = { showAddExp = true },
                        modifier = Modifier.weight(1f),
                        icon     = { Icon(Icons.Default.Add, null, Modifier.size(18.dp)) }
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Pie chart card ─────────────────────────────────────────────────
            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    OrbitCard {
                        Column(Modifier.padding(16.dp)) {
                            Text("Spending by Category", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Spacer(Modifier.height(16.dp))
                            if (uiState.totalExpenditure > 0) {
                                CategoryPieChart(
                                    expenseByCategory = uiState.expenseByCategory,
                                    categories        = uiState.budgetMonth?.categories ?: DEFAULT_BUDGET_CATEGORIES,
                                    modifier          = Modifier.size(180.dp).align(Alignment.CenterHorizontally)
                                )
                            } else {
                                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                    Text("No expenses this month", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Category cards ─────────────────────────────────────────────────
            val cats = uiState.budgetMonth?.categories ?: DEFAULT_BUDGET_CATEGORIES
            items(cats) { cat ->
                val spent = uiState.expenseByCategory[cat.name] ?: 0.0
                CategoryCard(
                    category = cat,
                    spent    = spent,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // ── Expense log link ───────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable { showExpLog = true }
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Receipt, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("See Expense Logs", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Dialogs
        if (showAddExp) {
            AddExpenseDialog(
                categories = uiState.budgetMonth?.categories ?: DEFAULT_BUDGET_CATEGORIES,
                daysInMonth = ym.lengthOfMonth(),
                onConfirm = { desc, amt, day, cat ->
                    viewModel.addExpense(desc, amt, day, cat)
                    showAddExp = false
                },
                onDismiss = { showAddExp = false }
            )
        }

        if (showModify) {
            ModifyBudgetDialog(
                current   = uiState.budgetMonth ?: BudgetMonth(uiState.monthKey),
                onConfirm = { viewModel.updateBudgetMonth(it); showModify = false },
                onDismiss = { showModify = false }
            )
        }
    }
}

// ── Expense log screen ────────────────────────────────────────────────────────
@Composable
fun ExpenseLogScreen(expenses: List<Expense>, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(start = 8.dp, end = 20.dp, top = 52.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            Text(
                text  = "Expense Log",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = BelfastGroteskBlackFamily,
                    fontWeight = FontWeight.Black,
                    color      = MaterialTheme.colorScheme.onBackground
                )
            )
        }

        if (expenses.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No expenses this month", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
            }
        } else {
            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(expenses) { expense ->
                    OrbitCard {
                        Row(
                            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.AttachMoney, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(expense.description, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${expense.category} · Day ${expense.dayOfMonth}", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            }
                            Text(
                                "₹%.2f".format(expense.amount),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Daily bar chart ───────────────────────────────────────────────────────────
@Composable
fun DailyBarChart(
    expenseByDay: Map<Int, Double>,
    daysInMonth: Int,
    modifier: Modifier = Modifier
) {
    val maxVal = expenseByDay.values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val barColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val totalWidth = size.width
        val totalHeight = size.height
        val barWidth = (totalWidth / daysInMonth) * 0.6f
        val gap      = (totalWidth / daysInMonth) * 0.4f
        val bottomPad = 20.dp.toPx()

        val paint = android.graphics.Paint().apply {
            color     = textColor.toArgb()
            textSize  = 8.dp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
        }

        for (day in 1..daysInMonth) {
            val amt     = expenseByDay[day] ?: 0.0
            val barH    = ((amt / maxVal) * (totalHeight - bottomPad)).toFloat()
            val x       = (day - 1) * (barWidth + gap)

            if (barH > 0) {
                drawRect(
                    color   = barColor,
                    topLeft = Offset(x, totalHeight - bottomPad - barH),
                    size    = Size(barWidth, barH)
                )
            }

            // Draw day label every 5 days
            if (day == 1 || day % 5 == 0) {
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        "$day",
                        x + barWidth / 2,
                        totalHeight,
                        paint
                    )
                }
            }
        }
    }
}

// ── Category pie chart ────────────────────────────────────────────────────────
val PIE_COLORS = listOf(
    Color(0xFFFFB08A), Color(0xFFA8E6CF), Color(0xFFFFD3B6),
    Color(0xFFFFAAA5), Color(0xFFB8D4E3), Color(0xFFD4BBFF),
    Color(0xFF6BCB77), Color(0xFF4ECDC4), Color(0xFFFFE66D)
)

@Composable
fun CategoryPieChart(
    expenseByCategory: Map<String, Double>,
    categories: List<BudgetCategory>,
    modifier: Modifier = Modifier
) {
    val total = expenseByCategory.values.sum().takeIf { it > 0 } ?: return
    val slices = categories.mapIndexedNotNull { i, cat ->
        val amt = expenseByCategory[cat.name] ?: 0.0
        if (amt > 0) Triple(cat.name, amt / total, PIE_COLORS[i % PIE_COLORS.size]) else null
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = modifier) {
            var startAngle = -90f
            val inset = 16.dp.toPx()
            slices.forEach { (_, frac, color) ->
                val sweep = (frac * 360f).toFloat()
                drawArc(
                    color      = color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter  = true,
                    topLeft    = Offset(inset, inset),
                    size       = Size(size.width - inset * 2, size.height - inset * 2)
                )
                startAngle += sweep
            }
        }
        Spacer(Modifier.height(12.dp))
        // Legend
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            slices.forEach { (name, frac, color) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
                    Text(
                        "$name (${"%.1f".format(frac * 100)}%)",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface)
                    )
                }
            }
        }
    }
}

// ── Category card ─────────────────────────────────────────────────────────────
@Composable
fun CategoryCard(category: BudgetCategory, spent: Double, modifier: Modifier = Modifier) {
    OrbitCard(modifier = modifier) {
        Row(
            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector        = categoryIcon(category.iconName),
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(24.dp)
            )
            Text(
                text     = category.name,
                style    = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "₹%.2f".format(spent),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
private fun categoryIcon(iconName: String) = when (iconName) {
    "restaurant"       -> Icons.Default.Restaurant
    "shopping_bag"     -> Icons.Default.ShoppingBag
    "receipt_long"     -> Icons.Default.ReceiptLong
    "movie"            -> Icons.Default.Movie
    "directions_car"   -> Icons.Default.DirectionsCar
    "favorite"         -> Icons.Default.Favorite
    "school"           -> Icons.Default.School
    else               -> Icons.Default.AttachMoney
}

// ── Budget stat widget ────────────────────────────────────────────────────────
@Composable
private fun BudgetStat(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = valueColor))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
    }
}

// ── Add expense dialog ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    categories: List<BudgetCategory>,
    daysInMonth: Int,
    onConfirm: (description: String, amount: Double, day: Int, category: String) -> Unit,
    onDismiss: () -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount      by remember { mutableStateOf("") }
    var day         by remember { mutableIntStateOf(LocalDate.now().dayOfMonth) }
    var category    by remember { mutableStateOf(categories.firstOrNull()?.name ?: "") }
    var expanded    by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("Add Expense", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Description") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text("Amount (₹)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = "$day", onValueChange = { v -> v.toIntOrNull()?.let { if (it in 1..daysInMonth) day = it } },
                    label = { Text("Day of month (1–$daysInMonth)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)
                )

                // Category dropdown
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value          = category,
                        onValueChange  = {},
                        readOnly       = true,
                        label          = { Text("Category") },
                        trailingIcon   = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier       = Modifier.fillMaxWidth().menuAnchor(),
                        shape          = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text     = { Text(cat.name) },
                                onClick  = { category = cat.name; expanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amt = amount.toDoubleOrNull() ?: return@TextButton
                if (description.isNotBlank() && amt > 0) onConfirm(description.trim(), amt, day, category)
            }) { Text("Add", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Discard") } }
    )
}

// ── Modify budget dialog ──────────────────────────────────────────────────────
@Composable
fun ModifyBudgetDialog(
    current: BudgetMonth,
    onConfirm: (BudgetMonth) -> Unit,
    onDismiss: () -> Unit
) {
    var budgetStr   by remember { mutableStateOf(current.budgetAmount.toInt().toString()) }
    var categories  by remember { mutableStateOf(current.categories.toMutableList()) }
    var newCatName  by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("Modify Budget", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = budgetStr, onValueChange = { budgetStr = it },
                    label = { Text("Monthly Budget (₹)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)
                )

                Text("Categories", style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))

                categories.forEachIndexed { idx, cat ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(cat.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { categories = (categories - cat).toMutableList() }) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newCatName, onValueChange = { newCatName = it },
                        label = { Text("New category") }, singleLine = true,
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)
                    )
                    IconButton(onClick = {
                        if (newCatName.isNotBlank() && categories.none { it.name == newCatName.trim() }) {
                            categories = (categories + BudgetCategory(newCatName.trim(), "attach_money")).toMutableList()
                            newCatName = ""
                        }
                    }) {
                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val budget = budgetStr.toDoubleOrNull() ?: current.budgetAmount
                onConfirm(current.copy(budgetAmount = budget, categories = categories))
            }) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Discard") } }
    )
}
