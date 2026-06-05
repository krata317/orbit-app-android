package com.krata.orbit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// ── Tasks ─────────────────────────────────────────────────────────────────────
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isCompleted: Boolean = false,
    val isUrgent: Boolean = false,
    val isRollover: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val position: Int = 0
)

// ── Events ────────────────────────────────────────────────────────────────────
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val dateTimeMillis: Long,           // epoch ms of the next occurrence
    val isCompleted: Boolean = false,
    val isRecurring: Boolean = false,
    val frequency: String = "none",     // none / daily / weekly / monthly
    val weekday: Int = -1,              // 1=Mon..7=Sun, -1 = not set
    val monthDays: String = "",         // comma-separated day-of-month numbers
    val createdAt: Long = System.currentTimeMillis()
)

// ── Habits ────────────────────────────────────────────────────────────────────
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val frequency: String = "daily",    // daily / weekly / monthly
    val weekday: Int = -1,
    val monthDays: String = "",
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val isCompletedToday: Boolean = false,
    val lastCompletedDate: String = "", // yyyy-MM-dd
    val createdAt: Long = System.currentTimeMillis()
)

// ── Budget: Monthly Snapshot ──────────────────────────────────────────────────
@Entity(tableName = "budget_month")
data class BudgetMonthEntity(
    @PrimaryKey val monthKey: String,   // "YYYY-MM"
    val budgetAmount: Double = 5000.0,
    val categories: String = ""         // JSON array of category objects
)

// ── Budget: Expense ───────────────────────────────────────────────────────────
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthKey: String,               // "YYYY-MM"
    val description: String,
    val amount: Double,
    val dayOfMonth: Int,
    val category: String,
    val createdAt: Long = System.currentTimeMillis()
)

// ── Coding: cached POTD ───────────────────────────────────────────────────────
@Entity(tableName = "potd_cache")
data class PotdCacheEntity(
    @PrimaryKey val site: String,       // "leetcode" | "gfg"
    val problemName: String,
    val difficulty: String,
    val url: String,
    val fetchedAt: Long = System.currentTimeMillis()
)

// ── Coding: cached Contests ───────────────────────────────────────────────────
@Entity(tableName = "contest_cache")
data class ContestCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val site: String,                   // "leetcode" | "codechef" | "codeforces"
    val title: String,
    val url: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val coverImage: String = "",
    val fetchedAt: Long = System.currentTimeMillis()
)
