package com.krata.orbit.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.krata.orbit.data.local.AppDatabase
import com.krata.orbit.data.model.Habit
import com.krata.orbit.data.model.HABIT_ACHIEVEMENT_DAYS
import com.krata.orbit.data.repository.HabitRepository
import com.krata.orbit.utils.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HabitsUiState(
    val habits: List<Habit>       = emptyList(),
    val isSelectionMode: Boolean  = false,
    val selectedIds: Set<Long>    = emptySet()
)

data class HabitDetailState(
    val habit: Habit,
    val unlockedAchievements: Set<Int>
)

class HabitsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = HabitRepository(AppDatabase.getInstance(application).habitDao())

    private val _isSelectionMode = MutableStateFlow(false)
    private val _selectedIds     = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<HabitsUiState> = combine(
        repo.getAllHabits(),
        _isSelectionMode,
        _selectedIds
    ) { habits, selMode, selIds ->
        HabitsUiState(
            habits          = habits,
            isSelectionMode = selMode,
            selectedIds     = selIds
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitsUiState())

    fun addHabit(habit: Habit) {
        viewModelScope.launch { repo.addHabit(habit) }
    }

    fun updateHabit(habit: Habit) {
        viewModelScope.launch { repo.updateHabit(habit) }
    }

    fun toggleHabit(habit: Habit) {
        viewModelScope.launch {
            val today = DateUtils.todayString()
            if (!habit.isCompletedToday) {
                val newStreak = habit.currentStreak + 1
                repo.updateHabit(
                    habit.copy(
                        isCompletedToday  = true,
                        lastCompletedDate = today,
                        currentStreak     = newStreak,
                        maxStreak         = maxOf(habit.maxStreak, newStreak)
                    )
                )
            } else {
                val newStreak = (habit.currentStreak - 1).coerceAtLeast(0)
                repo.updateHabit(
                    habit.copy(
                        isCompletedToday  = false,
                        currentStreak     = newStreak
                    )
                )
            }
        }
    }

    fun getHabitDetail(habitId: Long): Flow<HabitDetailState?> =
        repo.getAllHabits().map { list ->
            val habit = list.find { it.id == habitId } ?: return@map null
            val unlocked = HABIT_ACHIEVEMENT_DAYS.filter { it <= habit.maxStreak }.toSet()
            HabitDetailState(habit = habit, unlockedAchievements = unlocked)
        }

    fun enterSelectionMode()  { _isSelectionMode.value = true }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedIds.value     = emptySet()
    }

    fun toggleSelection(id: Long) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (contains(id)) remove(id) else add(id)
        }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            repo.deleteHabitsByIds(_selectedIds.value.toList())
            exitSelectionMode()
        }
    }
}
