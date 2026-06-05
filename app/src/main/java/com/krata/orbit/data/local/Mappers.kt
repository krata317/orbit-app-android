package com.krata.orbit.data.local

import com.krata.orbit.data.local.entity.*
import com.krata.orbit.data.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

// ── Task mappers ──────────────────────────────────────────────────────────────
fun TaskEntity.toDomain() = Task(
    id          = id,
    name        = name,
    isCompleted = isCompleted,
    isUrgent    = isUrgent,
    isRollover  = isRollover,
    position    = position
)

fun Task.toEntity() = TaskEntity(
    id          = id,
    name        = name,
    isCompleted = isCompleted,
    isUrgent    = isUrgent,
    isRollover  = isRollover,
    position    = position
)

// ── Event mappers ─────────────────────────────────────────────────────────────
fun EventEntity.toDomain() = Event(
    id              = id,
    name            = name,
    description     = description,
    dateTimeMillis  = dateTimeMillis,
    isCompleted     = isCompleted,
    isRecurring     = isRecurring,
    frequency       = EventFrequency.valueOf(frequency.uppercase().ifEmpty { "NONE" }),
    weekday         = weekday,
    monthDays       = if (monthDays.isBlank()) emptyList()
                      else monthDays.split(",").mapNotNull { it.trim().toIntOrNull() }
)

fun Event.toEntity() = EventEntity(
    id              = id,
    name            = name,
    description     = description,
    dateTimeMillis  = dateTimeMillis,
    isCompleted     = isCompleted,
    isRecurring     = isRecurring,
    frequency       = frequency.name.lowercase(),
    weekday         = weekday,
    monthDays       = monthDays.joinToString(",")
)

// ── Habit mappers ─────────────────────────────────────────────────────────────
fun HabitEntity.toDomain() = Habit(
    id                = id,
    name              = name,
    frequency         = HabitFrequency.valueOf(frequency.uppercase().ifEmpty { "DAILY" }),
    weekday           = weekday,
    monthDays         = if (monthDays.isBlank()) emptyList()
                        else monthDays.split(",").mapNotNull { it.trim().toIntOrNull() },
    currentStreak     = currentStreak,
    maxStreak         = maxStreak,
    isCompletedToday  = isCompletedToday,
    lastCompletedDate = lastCompletedDate
)

fun Habit.toEntity() = HabitEntity(
    id                = id,
    name              = name,
    frequency         = frequency.name.lowercase(),
    weekday           = weekday,
    monthDays         = monthDays.joinToString(","),
    currentStreak     = currentStreak,
    maxStreak         = maxStreak,
    isCompletedToday  = isCompletedToday,
    lastCompletedDate = lastCompletedDate
)

// ── Budget mappers ────────────────────────────────────────────────────────────
@kotlinx.serialization.Serializable
private data class CategoryJson(val name: String, val iconName: String)

fun BudgetMonthEntity.toDomain(): BudgetMonth {
    val cats = try {
        json.decodeFromString<List<CategoryJson>>(categories.ifBlank { "[]" })
            .map { BudgetCategory(it.name, it.iconName) }
    } catch (_: Exception) {
        DEFAULT_BUDGET_CATEGORIES
    }
    return BudgetMonth(
        monthKey      = monthKey,
        budgetAmount  = budgetAmount,
        categories    = cats.ifEmpty { DEFAULT_BUDGET_CATEGORIES }
    )
}

fun BudgetMonth.toEntity(): BudgetMonthEntity {
    val catsJson = json.encodeToString(
        categories.map { CategoryJson(it.name, it.iconName) }
    )
    return BudgetMonthEntity(
        monthKey      = monthKey,
        budgetAmount  = budgetAmount,
        categories    = catsJson
    )
}

// ── Expense mappers ───────────────────────────────────────────────────────────
fun ExpenseEntity.toDomain() = Expense(
    id          = id,
    monthKey    = monthKey,
    description = description,
    amount      = amount,
    dayOfMonth  = dayOfMonth,
    category    = category,
    createdAt   = createdAt
)

fun Expense.toEntity() = ExpenseEntity(
    id          = id,
    monthKey    = monthKey,
    description = description,
    amount      = amount,
    dayOfMonth  = dayOfMonth,
    category    = category,
    createdAt   = createdAt
)

// ── Coding cache mappers ──────────────────────────────────────────────────────
fun PotdCacheEntity.toDomain() = PotdItem(
    site        = site,
    problemName = problemName,
    difficulty  = difficulty,
    url         = url
)

fun ContestCacheEntity.toDomain() = ContestItem(
    id              = id,
    site            = site,
    title           = title,
    url             = url,
    startTimeMillis = startTimeMillis,
    endTimeMillis   = endTimeMillis,
    coverImage      = coverImage
)
