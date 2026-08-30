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

    // ===== СЦЕНАРИЙ 2: Реактивная подписка (collect) =====
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

        // ===== СЦЕНАРИЙ 5: Debounce =====
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

    // ===== СЦЕНАРИЙ 1: Фоновая работа (withContext) =====
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
                _uiState.value = MainUiState.Error("No internet: ${e.message}")
            } catch (e: Exception) {
                _uiState.value = MainUiState.Error("Error: ${e.message}")
            }
        }
    }

    // ===== СЦЕНАРИЙ 3: Параллельный запуск (async) =====
    fun loadParallel() {
        viewModelScope.launch {
            _uiState.value = MainUiState.Loading
            try {
                val result = coroutineScope {
                    val userDeferred = async(Dispatchers.IO) { repository.getUser(1) }
                    val settingsDeferred = async(Dispatchers.IO) { repository.fetchSettings() }
                    Pair(userDeferred.await(), settingsDeferred.await())
                }
                val (user, settings) = result
                _uiState.value = MainUiState.Content(
                    users = listOf(user),
                    settings = settings
                )
                _events.emit(MainEvent.ShowToast("Параллельная загрузка завершена!"))
            } catch (e: Exception) {
                _uiState.value = MainUiState.Error("Parallel error: ${e.message}")
            }
        }
    }

    // ===== СЦЕНАРИЙ 4: Отмена задачи =====
    fun cancelLoading() {
        loadJob?.cancel()
        _uiState.value = MainUiState.Content(
            users = emptyList(),
            searchResults = emptyList(),
            settings = null
        )
        viewModelScope.launch {
            _events.emit(MainEvent.ShowToast("Загрузка отменена!"))
        }
    }

    // ===== СЦЕНАРИЙ 5: Задержка (delay) =====
    fun showTemporaryMessage() {
        viewModelScope.launch {
            _events.emit(MainEvent.ShowToast("Сообщение появилось!"))
            delay(3000)
            _events.emit(MainEvent.HideToast)
            _events.emit(MainEvent.ShowToast("Сообщение скрыто через 3 секунды"))
        }
    }

    // ===== СЦЕНАРИЙ 6: Обработка ошибок =====
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = MainUiState.Error("Critical: ${throwable.message}")
    }

    fun riskyOperation() {
        val scope = CoroutineScope(Dispatchers.IO + exceptionHandler)
        scope.launch {
            repository.getUser(-1) // Вызовет исключение
        }
    }

    // ===== СЦЕНАРИЙ 6.3: SupervisorJob =====
    fun loadIndependentData() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            repository.getUser(-1) // Эта задача упадет
        }
        scope.launch {
            val settings = repository.fetchSettings() // Эта задача выполнится
            _uiState.update {
                (it as? MainUiState.Content)?.copy(settings = settings)
                    ?: MainUiState.Content(settings = settings)
            }
            viewModelScope.launch {
                _events.emit(MainEvent.ShowToast("Настройки загружены независимо!"))
            }
        }
    }

    // ===== ДЛЯ ОТЛАДКИ (force состояния) =====
    fun forceContent() {
        _uiState.value = MainUiState.Content(
            users = listOf(
                User(1, "Test User 1", "test1@test.com"),
                User(2, "Test User 2", "test2@test.com")
            ),
            searchResults = listOf(
                User(3, "Found User", "found@test.com")
            ),
            settings = Settings(true, "en")
        )
    }

    // ===== Вспомогательные методы =====
    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }
}