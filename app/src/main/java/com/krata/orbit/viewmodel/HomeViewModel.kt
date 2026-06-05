package com.krata.orbit.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.krata.orbit.data.local.AppDatabase
import com.krata.orbit.data.model.Task
import com.krata.orbit.data.repository.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val tasks: List<Task>     = emptyList(),
    val totalTasks: Int       = 0,
    val completedTasks: Int   = 0,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long>   = emptySet()
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = TaskRepository(AppDatabase.getInstance(application).taskDao())

    private val _isSelectionMode = MutableStateFlow(false)
    private val _selectedIds     = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<HomeUiState> = combine(
        repo.getAllTasks(),
        _isSelectionMode,
        _selectedIds
    ) { tasks, selMode, selIds ->
        HomeUiState(
            tasks           = tasks,
            totalTasks      = tasks.size,
            completedTasks  = tasks.count { it.isCompleted },
            isSelectionMode = selMode,
            selectedIds     = selIds
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun addTask(name: String, isUrgent: Boolean, isRollover: Boolean) {
        viewModelScope.launch {
            val position = uiState.value.tasks.size
            repo.addTask(Task(name = name, isUrgent = isUrgent, isRollover = isRollover, position = position))
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            repo.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch { repo.updateTask(task) }
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
            repo.deleteTasksByIds(_selectedIds.value.toList())
            exitSelectionMode()
        }
    }
}
