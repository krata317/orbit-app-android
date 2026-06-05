package com.krata.orbit.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.krata.orbit.data.local.AppDatabase
import com.krata.orbit.data.model.ContestItem
import com.krata.orbit.data.model.PotdItem
import com.krata.orbit.data.repository.CodingRepository
import com.krata.orbit.utils.NetworkUtils
import com.krata.orbit.workers.CodingRefreshWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CodingUiState(
    val potdList: List<PotdItem>       = emptyList(),
    val contests: List<ContestItem>    = emptyList(),
    val isConnected: Boolean           = false,
    val isRefreshing: Boolean          = false
)

class CodingViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = CodingRepository(AppDatabase.getInstance(application).codingCacheDao())
    private val _isRefreshing = MutableStateFlow(false)
    private val connectivity  = NetworkUtils.observeConnectivity(application)

    val uiState: StateFlow<CodingUiState> = combine(
        repo.getAllPotd(),
        repo.getAllContests(),
        connectivity,
        _isRefreshing
    ) { potd, contests, connected, refreshing ->
        CodingUiState(
            potdList     = potd,
            contests     = contests,
            isConnected  = connected,
            isRefreshing = refreshing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CodingUiState())

    init {
        // Auto-refresh whenever network becomes available
        viewModelScope.launch {
            connectivity.collect { connected ->
                if (connected) triggerRefresh()
            }
        }
    }

    fun triggerRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            CodingRefreshWorker.scheduleOnce(getApplication())
            _isRefreshing.value = false
        }
    }
}
