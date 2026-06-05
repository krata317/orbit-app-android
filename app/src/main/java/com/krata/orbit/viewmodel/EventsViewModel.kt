package com.krata.orbit.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.krata.orbit.data.local.AppDatabase
import com.krata.orbit.data.model.Event
import com.krata.orbit.data.repository.EventRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EventsUiState(
    val events: List<Event>       = emptyList(),
    val isSelectionMode: Boolean  = false,
    val selectedIds: Set<Long>    = emptySet()
)

class EventsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = EventRepository(AppDatabase.getInstance(application).eventDao())

    private val _isSelectionMode = MutableStateFlow(false)
    private val _selectedIds     = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<EventsUiState> = combine(
        repo.getAllEvents(),
        _isSelectionMode,
        _selectedIds
    ) { events, selMode, selIds ->
        EventsUiState(
            events          = events,
            isSelectionMode = selMode,
            selectedIds     = selIds
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventsUiState())

    fun addEvent(event: Event) {
        viewModelScope.launch { repo.addEvent(event) }
    }

    fun updateEvent(event: Event) {
        viewModelScope.launch { repo.updateEvent(event) }
    }

    fun toggleEvent(event: Event) {
        viewModelScope.launch { repo.updateEvent(event.copy(isCompleted = !event.isCompleted)) }
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
            repo.deleteEventsByIds(_selectedIds.value.toList())
            exitSelectionMode()
        }
    }
}
