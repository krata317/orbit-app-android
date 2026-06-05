package com.krata.orbit.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.krata.orbit.data.api.CodingApiService
import com.krata.orbit.data.local.AppDatabase
import com.krata.orbit.data.repository.CodingRepository
import com.krata.orbit.data.repository.EventRepository
import com.krata.orbit.data.repository.HabitRepository
import com.krata.orbit.data.repository.TaskRepository
import com.krata.orbit.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

private const val TAG = "Workers"

// ── Midnight Reset Worker ─────────────────────────────────────────────────────
class MidnightResetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db           = AppDatabase.getInstance(applicationContext)
            val taskRepo     = TaskRepository(db.taskDao())
            val eventRepo    = EventRepository(db.eventDao())
            val habitRepo    = HabitRepository(db.habitDao())

            val nowMillis    = System.currentTimeMillis()
            val today        = LocalDate.now().toString()   // yyyy-MM-dd

            // 1. Delete non-rollover tasks; reset rollover ones
            taskRepo.deleteNonRolloverTasks()
            taskRepo.resetRolloverTasks()

            // 2. Delete expired one-time events
            val expiredOneTime = eventRepo.getExpiredOneTimeEvents(nowMillis)
            expiredOneTime.forEach { eventRepo.deleteEvent(it) }

            // 3. Advance recurring events whose date has passed
            val recurring = eventRepo.getAllRecurringEvents()
            recurring.forEach { event ->
                if (event.dateTimeMillis < nowMillis) {
                    val nextMillis = DateUtils.nextOccurrenceMillis(event)
                    eventRepo.updateEvent(event.copy(dateTimeMillis = nextMillis, isCompleted = false))
                }
            }

            // 4. Reset habits (mark uncompleted for today if applicable)
            val habits = habitRepo.getAllHabitsOnce()
            habits.forEach { habit ->
                if (habit.lastCompletedDate != today) {
                    // If habit was due yesterday but not completed, reset streak only if it was missed
                    val wasCompletedYesterday = habit.lastCompletedDate ==
                        LocalDate.now().minusDays(1).toString()
                    val newStreak = if (habit.isCompletedToday || wasCompletedYesterday)
                        habit.currentStreak else 0
                    habitRepo.updateHabit(
                        habit.copy(
                            isCompletedToday = false,
                            currentStreak    = newStreak,
                            maxStreak        = maxOf(habit.maxStreak, newStreak)
                        )
                    )
                }
            }

            Log.d(TAG, "MidnightResetWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "MidnightResetWorker failed: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "midnight_reset"

        fun schedule(context: Context) {
            val now          = LocalDateTime.now(ZoneId.systemDefault())
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(ZoneId.systemDefault())
            val delayMillis  = nextMidnight.toInstant().toEpochMilli() - System.currentTimeMillis()

            val request = PeriodicWorkRequestBuilder<MidnightResetWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder().build())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

// ── Coding Refresh Worker ─────────────────────────────────────────────────────
class CodingRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db          = AppDatabase.getInstance(applicationContext)
            val codingRepo  = CodingRepository(db.codingCacheDao())

            // Fetch POTD
            CodingApiService.fetchLeetCodePotd()?.let { codingRepo.cachePotd(it) }
            CodingApiService.fetchGfgPotd()?.let { codingRepo.cachePotd(it) }

            // Fetch Contests
            codingRepo.cacheContests("leetcode",   CodingApiService.fetchLeetCodeContests())
            codingRepo.cacheContests("codechef",   CodingApiService.fetchCodeChefContests())
            codingRepo.cacheContests("codeforces", CodingApiService.fetchCodeforcesContests())

            Log.d(TAG, "CodingRefreshWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "CodingRefreshWorker failed: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "coding_refresh"

        fun scheduleOnce(context: Context) {
            val request = OneTimeWorkRequestBuilder<CodingRefreshWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}

// ── Budget Month Reset Worker ─────────────────────────────────────────────────
class BudgetMonthResetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db         = AppDatabase.getInstance(applicationContext)
            val budgetDao  = db.budgetDao()
            val priorMonth = DateUtils.monthKeyForOffset(-1) // previous month
            budgetDao.deleteExpensesForMonth(priorMonth)
            budgetDao.deleteBudgetMonthRecord(priorMonth)
            Log.d(TAG, "BudgetMonthResetWorker cleared $priorMonth")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "BudgetMonthResetWorker failed: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "budget_month_reset"

        fun schedule(context: Context) {
            // Run once at the start of every month (scheduled as monthly via periodic)
            val request = PeriodicWorkRequestBuilder<BudgetMonthResetWorker>(30, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().build())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
