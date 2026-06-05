package com.krata.orbit.data.model

// ── Tasks ─────────────────────────────────────────────────────────────────────
data class Task(
    val id: Long = 0,
    val name: String,
    val isCompleted: Boolean = false,
    val isUrgent: Boolean = false,
    val isRollover: Boolean = false,
    val position: Int = 0
)

// ── Events ────────────────────────────────────────────────────────────────────
data class Event(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val dateTimeMillis: Long,
    val isCompleted: Boolean = false,
    val isRecurring: Boolean = false,
    val frequency: EventFrequency = EventFrequency.NONE,
    val weekday: Int = -1,
    val monthDays: List<Int> = emptyList()
)

enum class EventFrequency { NONE, DAILY, WEEKLY, MONTHLY }

// ── Habits ────────────────────────────────────────────────────────────────────
data class Habit(
    val id: Long = 0,
    val name: String,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val weekday: Int = -1,
    val monthDays: List<Int> = emptyList(),
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val isCompletedToday: Boolean = false,
    val lastCompletedDate: String = ""
)

enum class HabitFrequency { DAILY, WEEKLY, MONTHLY }

val HABIT_ACHIEVEMENT_DAYS = listOf(1, 3, 5, 7, 10, 21, 50, 69, 75, 100, 200, 365, 500)

// ── Budget ────────────────────────────────────────────────────────────────────
data class BudgetCategory(
    val name: String,
    val iconName: String
)

val DEFAULT_BUDGET_CATEGORIES = listOf(
    BudgetCategory("Food & Drinks",   "restaurant"),
    BudgetCategory("Shopping",        "shopping_bag"),
    BudgetCategory("Bills",           "receipt_long"),
    BudgetCategory("Entertainment",   "movie"),
    BudgetCategory("Transport",       "directions_car"),
    BudgetCategory("Health",          "favorite"),
    BudgetCategory("Education",       "school")
)

data class Expense(
    val id: Long = 0,
    val monthKey: String,
    val description: String,
    val amount: Double,
    val dayOfMonth: Int,
    val category: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class BudgetMonth(
    val monthKey: String,
    val budgetAmount: Double = 5000.0,
    val categories: List<BudgetCategory> = DEFAULT_BUDGET_CATEGORIES
)

// ── Coding ────────────────────────────────────────────────────────────────────
data class PotdItem(
    val site: String,
    val problemName: String,
    val difficulty: String,
    val url: String
)

data class ContestItem(
    val id: Long = 0,
    val site: String,
    val title: String,
    val url: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val coverImage: String = ""
)
