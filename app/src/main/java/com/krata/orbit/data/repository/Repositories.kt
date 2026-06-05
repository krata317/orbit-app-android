package com.krata.orbit.data.repository

import com.krata.orbit.data.api.ApiContest
import com.krata.orbit.data.api.ApiPotd
import com.krata.orbit.data.local.*
import com.krata.orbit.data.local.dao.*
import com.krata.orbit.data.local.entity.*
import com.krata.orbit.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ── Task Repository ───────────────────────────────────────────────────────────
class TaskRepository(private val dao: TaskDao) {
    fun getAllTasks(): Flow<List<Task>> = dao.getAllTasks().map { list -> list.map { it.toDomain() } }
    suspend fun addTask(task: Task): Long = dao.insertTask(task.toEntity())
    suspend fun updateTask(task: Task) = dao.updateTask(task.toEntity())
    suspend fun deleteTask(task: Task) = dao.deleteTask(task.toEntity())
    suspend fun deleteTasksByIds(ids: List<Long>) = dao.deleteTasksByIds(ids)
    suspend fun deleteNonRolloverTasks() = dao.deleteNonRolloverTasks()
    suspend fun resetRolloverTasks() = dao.resetRolloverTasks()
    suspend fun getTotalCount(): Int = dao.getTotalTaskCount()
    suspend fun getCompletedCount(): Int = dao.getCompletedTaskCount()
}

// ── Event Repository ──────────────────────────────────────────────────────────
class EventRepository(private val dao: EventDao) {
    fun getAllEvents(): Flow<List<Event>> = dao.getAllEvents().map { list -> list.map { it.toDomain() } }
    suspend fun addEvent(event: Event): Long = dao.insertEvent(event.toEntity())
    suspend fun updateEvent(event: Event) = dao.updateEvent(event.toEntity())
    suspend fun deleteEvent(event: Event) = dao.deleteEvent(event.toEntity())
    suspend fun deleteEventsByIds(ids: List<Long>) = dao.deleteEventsByIds(ids)
    suspend fun getExpiredOneTimeEvents(nowMillis: Long): List<Event> =
        dao.getExpiredOneTimeEvents(nowMillis).map { it.toDomain() }
    suspend fun getAllRecurringEvents(): List<Event> =
        dao.getAllRecurringEvents().map { it.toDomain() }
}

// ── Habit Repository ──────────────────────────────────────────────────────────
class HabitRepository(private val dao: HabitDao) {
    fun getAllHabits(): Flow<List<Habit>> = dao.getAllHabits().map { list -> list.map { it.toDomain() } }
    suspend fun addHabit(habit: Habit): Long = dao.insertHabit(habit.toEntity())
    suspend fun updateHabit(habit: Habit) = dao.updateHabit(habit.toEntity())
    suspend fun deleteHabit(habit: Habit) = dao.deleteHabit(habit.toEntity())
    suspend fun deleteHabitsByIds(ids: List<Long>) = dao.deleteHabitsByIds(ids)
    suspend fun getAllHabitsOnce(): List<Habit> = dao.getAllHabitsOnce().map { it.toDomain() }
}

// ── Budget Repository ─────────────────────────────────────────────────────────
class BudgetRepository(private val dao: BudgetDao) {
    fun getBudgetMonth(monthKey: String): Flow<BudgetMonth?> =
        dao.getBudgetMonth(monthKey).map { it?.toDomain() }

    suspend fun getBudgetMonthOnce(monthKey: String): BudgetMonth? =
        dao.getBudgetMonthOnce(monthKey)?.toDomain()

    suspend fun saveBudgetMonth(budget: BudgetMonth) =
        dao.insertBudgetMonth(budget.toEntity())

    fun getExpenses(monthKey: String): Flow<List<Expense>> =
        dao.getExpensesForMonth(monthKey).map { list -> list.map { it.toDomain() } }

    suspend fun addExpense(expense: Expense) = dao.insertExpense(expense.toEntity())

    suspend fun resetMonth(monthKey: String) {
        dao.deleteExpensesForMonth(monthKey)
        dao.deleteBudgetMonthRecord(monthKey)
    }
}

// ── Coding Repository ─────────────────────────────────────────────────────────
class CodingRepository(private val dao: CodingCacheDao) {

    fun getAllPotd(): Flow<List<PotdItem>> =
        dao.getAllPotd().map { list -> list.map { it.toDomain() } }

    fun getAllContests(): Flow<List<ContestItem>> =
        dao.getAllContests().map { list -> list.map { it.toDomain() } }

    suspend fun cachePotd(apiPotd: ApiPotd) =
        dao.insertPotd(
            PotdCacheEntity(
                site        = apiPotd.site,
                problemName = apiPotd.problemName,
                difficulty  = apiPotd.difficulty,
                url         = apiPotd.url
            )
        )

    suspend fun cacheContests(site: String, contests: List<ApiContest>) {
        dao.deleteContestsBySite(site)
        contests.forEach { c ->
            dao.insertContest(
                ContestCacheEntity(
                    site            = c.site,
                    title           = c.title,
                    url             = c.url,
                    startTimeMillis = c.startTimeMillis,
                    endTimeMillis   = c.endTimeMillis,
                    coverImage      = c.coverImage
                )
            )
        }
    }
}
