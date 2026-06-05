package com.krata.orbit.data.local.dao

import androidx.room.*
import com.krata.orbit.data.local.entity.*
import kotlinx.coroutines.flow.Flow

// ── Tasks DAO ─────────────────────────────────────────────────────────────────
@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isUrgent DESC, isCompleted ASC, position ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id IN (:ids)")
    suspend fun deleteTasksByIds(ids: List<Long>)

    @Query("DELETE FROM tasks WHERE isRollover = 0")
    suspend fun deleteNonRolloverTasks()

    @Query("UPDATE tasks SET isCompleted = 0 WHERE isRollover = 1")
    suspend fun resetRolloverTasks()

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTotalTaskCount(): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1")
    suspend fun getCompletedTaskCount(): Int
}

// ── Events DAO ────────────────────────────────────────────────────────────────
@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY isCompleted ASC, dateTimeMillis ASC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity): Long

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Delete
    suspend fun deleteEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE id IN (:ids)")
    suspend fun deleteEventsByIds(ids: List<Long>)

    @Query("SELECT * FROM events WHERE isRecurring = 0 AND dateTimeMillis < :nowMillis")
    suspend fun getExpiredOneTimeEvents(nowMillis: Long): List<EventEntity>

    @Query("SELECT * FROM events WHERE isRecurring = 1")
    suspend fun getAllRecurringEvents(): List<EventEntity>
}

// ── Habits DAO ────────────────────────────────────────────────────────────────
@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY isCompletedToday ASC, name ASC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id IN (:ids)")
    suspend fun deleteHabitsByIds(ids: List<Long>)

    @Query("SELECT * FROM habits")
    suspend fun getAllHabitsOnce(): List<HabitEntity>
}

// ── Budget DAO ────────────────────────────────────────────────────────────────
@Dao
interface BudgetDao {
    @Query("SELECT * FROM budget_month WHERE monthKey = :monthKey LIMIT 1")
    fun getBudgetMonth(monthKey: String): Flow<BudgetMonthEntity?>

    @Query("SELECT * FROM budget_month WHERE monthKey = :monthKey LIMIT 1")
    suspend fun getBudgetMonthOnce(monthKey: String): BudgetMonthEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetMonth(budget: BudgetMonthEntity)

    @Query("SELECT * FROM expenses WHERE monthKey = :monthKey ORDER BY createdAt ASC")
    fun getExpensesForMonth(monthKey: String): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE monthKey = :monthKey")
    suspend fun deleteExpensesForMonth(monthKey: String)

    @Query("DELETE FROM budget_month WHERE monthKey = :monthKey")
    suspend fun deleteBudgetMonthRecord(monthKey: String)
}

// ── Coding Cache DAO ──────────────────────────────────────────────────────────
@Dao
interface CodingCacheDao {
    @Query("SELECT * FROM potd_cache")
    fun getAllPotd(): Flow<List<PotdCacheEntity>>

    @Query("SELECT * FROM potd_cache")
    suspend fun getAllPotdOnce(): List<PotdCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPotd(potd: PotdCacheEntity)

    @Query("SELECT * FROM contest_cache ORDER BY startTimeMillis ASC")
    fun getAllContests(): Flow<List<ContestCacheEntity>>

    @Query("SELECT * FROM contest_cache ORDER BY startTimeMillis ASC")
    suspend fun getAllContestsOnce(): List<ContestCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContest(contest: ContestCacheEntity)

    @Query("DELETE FROM contest_cache WHERE site = :site")
    suspend fun deleteContestsBySite(site: String)

    @Query("DELETE FROM contest_cache")
    suspend fun deleteAllContests()
}
