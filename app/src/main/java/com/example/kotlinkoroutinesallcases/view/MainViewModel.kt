package com.example.kotlinkoroutinesallcases.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlinkoroutinesallcases.data.Settings
import com.example.kotlinkoroutinesallcases.data.User
import com.example.kotlinkoroutinesallcases.repository.UserRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException

class MainViewModel(
    private val repository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState

    private val _events = MutableSharedFlow<MainEvent>()
    val events: SharedFlow<MainEvent> = _events

    private val searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            repository.getUsersFlow()
                .collect { users ->
                    _uiState.update {
                        (it as? MainUiState.Content)?.copy(users = users)
                            ?: MainUiState.Content(users = users)
                    }
                }
        }

        viewModelScope.launch {
            searchQuery
                .debounce(500)
                .distinctUntilChanged()
                .filter { it.length >= 3 }
                .mapLatest { query ->
                    repository.searchUsers(query)
                }
                .collect { results ->
                    _uiState.update {
                        (it as? MainUiState.Content)?.copy(searchResults = results)
                            ?: MainUiState.Content(searchResults = results)
                    }
                }
        }
    }

    suspend fun loadDataParallel(): Pair<User, Settings> {
        return coroutineScope {
            val userDeferred = async(Dispatchers.IO) { repository.getUser(1) }
            val settingsDeferred = async(Dispatchers.IO) { repository.fetchSettings() }
            Pair(userDeferred.await(), settingsDeferred.await())
        }
    }

    private var loadJob: Job? = null

    fun loadUserWithCancel() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = MainUiState.Loading
            try {
                val user = withContext(Dispatchers.IO) {
                    repository.getUser(1)
                }
                _uiState.value = MainUiState.Success(user)
            } catch (e: IOException) {
                _uiState.value = MainUiState.Error("No internet")
            } catch (e: Exception) {
                _uiState.value = MainUiState.Error("Error: ${e.message}")
            }
        }
    }

    fun showTemporaryMessage() {
        viewModelScope.launch {
            _events.emit(MainEvent.ShowToast("Message appears!"))
            delay(3000)
            _events.emit(MainEvent.HideToast)
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun riskyOperation() {
        val scope = CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            _uiState.value = MainUiState.Error("Critical: ${throwable.message}")
        })
        scope.launch {
            repository.getUser(-1)
        }
    }

    fun loadIndependentData() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            repository.getUser(-1)
        }
        scope.launch {
            val settings = repository.fetchSettings()
            _uiState.update {
                (it as? MainUiState.Content)?.copy(settings = settings)
                    ?: MainUiState.Content(settings = settings)
            }
        }
    }

    suspend fun fetchSettings(): Settings = withContext(Dispatchers.IO) {
        repository.fetchSettings()
    }
}