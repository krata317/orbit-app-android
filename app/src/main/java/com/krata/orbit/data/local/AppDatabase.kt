package com.krata.orbit.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.krata.orbit.data.local.dao.*
import com.krata.orbit.data.local.entity.*

@Database(
    entities = [
        TaskEntity::class,
        EventEntity::class,
        HabitEntity::class,
        BudgetMonthEntity::class,
        ExpenseEntity::class,
        PotdCacheEntity::class,
        ContestCacheEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun eventDao(): EventDao
    abstract fun habitDao(): HabitDao
    abstract fun budgetDao(): BudgetDao
    abstract fun codingCacheDao(): CodingCacheDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "orbit_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
