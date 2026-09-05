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

    // ============================================================
    //  🏗️  СОСТОЯНИЯ И ПОТОКИ
    // ============================================================

    // Состояние экрана (StateFlow)
    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState

    // События (SharedFlow) для тостов и навигации
    private val _events = MutableSharedFlow<MainEvent>()
    val events: SharedFlow<MainEvent> = _events

    // Поток для поискового запроса (debounce)
    private val searchQuery = MutableStateFlow("")

    // Job для управления загрузкой (отмена)
    private var loadJob: Job? = null

    val users: StateFlow<List<User>> = repository.getUsersFlow()
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ============================================================
    //  🚀  СЦЕНАРИЙ 2: РЕАКТИВНАЯ ПОДПИСКА (collect)
    // ============================================================

    init {
        // Подписка на изменения в БД (Flow)
        viewModelScope.launch {
            users.collect { userList ->
                println("✅ Подписка на изменения в БД (Flow)")
                println("✅ COLLECT: получено ${userList.size} пользователей")  // ← ЛОГ
                _uiState.update { currentState ->
                    when (currentState) {
                        is MainUiState.Content -> currentState.copy(users = userList)
                        else -> MainUiState.Content(users = userList)
                    }
                }
            }
        }

        // Поиск с debounce (Сценарий 5)
        viewModelScope.launch {
            searchQuery
                .debounce(500)                 // Ждём 500 мс после остановки печати
                .distinctUntilChanged()        // Не реагируем на повторяющиеся запросы
                .filter { it.length >= 3 }     // Игнорируем короткие запросы (< 3 символов)
                .mapLatest { query ->          // Отменяем предыдущий запрос, если пошёл новый
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

    // ============================================================
    //  🧵  СЦЕНАРИЙ 1: ФОНОВАЯ РАБОТА (withContext)
    // ============================================================

    fun loadUserWithCancel() {
        // Отменяем предыдущую загрузку, если она была
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = MainUiState.Loading
            try {
                // Репозиторий сам переключится на Dispatchers.IO
                val user = repository.getUser(1)
                _uiState.value = MainUiState.Success(user)
            } catch (e: IOException) {
                _uiState.value = MainUiState.Error("Нет интернета: ${e.message}")
            } catch (e: Exception) {
                _uiState.value = MainUiState.Error("Ошибка: ${e.message}")
            }
        }
    }

    // ============================================================
    //  ⚡  СЦЕНАРИЙ 3: ПАРАЛЛЕЛЬНЫЙ ЗАПУСК (async/await)
    // ============================================================

    fun loadParallel() {
        viewModelScope.launch {
            _uiState.value = MainUiState.Loading
            try {
                val result = coroutineScope {
                    // Запускаем две задачи параллельно
                    val userDeferred = async { repository.getUser(1) }
                    val settingsDeferred = async { repository.fetchSettings() }
                    Pair(userDeferred.await(), settingsDeferred.await())
                }
                val (user, settings) = result
                _uiState.value = MainUiState.Content(
                    users = listOf(user),
                    settings = settings
                )
                _events.emit(MainEvent.ShowToast("✅ Параллельная загрузка завершена!"))
            } catch (e: Exception) {
                _uiState.value = MainUiState.Error("Ошибка параллельной загрузки: ${e.message}")
            }
        }
    }

    // ============================================================
    //  🛑  СЦЕНАРИЙ 4: ОТМЕНА ЗАДАЧИ (job.cancel)
    // ============================================================

    fun cancelLoading() {
        loadJob?.cancel()
        _uiState.value = MainUiState.Content(
            users = emptyList(),
            searchResults = emptyList(),
            settings = null
        )
        viewModelScope.launch {
            _events.emit(MainEvent.ShowToast("⛔ Загрузка отменена!"))
        }
    }

    // ============================================================
    //  ⏳  СЦЕНАРИЙ 5: ЗАДЕРЖКА (delay)
    // ============================================================

    fun showTemporaryMessage() {
        viewModelScope.launch {
            _events.emit(MainEvent.ShowToast("🔔 Сообщение появилось!"))
            delay(3000)
            _events.emit(MainEvent.HideToast)
            _events.emit(MainEvent.ShowToast("🔇 Сообщение скрыто через 3 секунды"))
        }
    }

    // ============================================================
    //  🚨  СЦЕНАРИЙ 6: ОБРАБОТКА ОШИБОК (try-catch + CoroutineExceptionHandler)
    // ============================================================

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = MainUiState.Error("❌ Критическая ошибка: ${throwable.message}")
    }

    fun riskyOperation() {
        val scope = CoroutineScope(Dispatchers.IO + exceptionHandler)
        scope.launch {
            // Этот вызов выбросит исключение (передан id = -1)
            repository.getUser(-1)
        }
    }

    // ============================================================
    //  🧩  СЦЕНАРИЙ 6.3: SUPERVISORJOB (изоляция ошибок)
    // ============================================================

    fun loadIndependentData() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        // Задача 1: упадет (id = -1), но не убьет соседнюю задачу
        scope.launch {
            try {
                repository.getUser(-1)
            } catch (throwable: Exception) {
                _uiState.value = MainUiState.Error("❌ Критическая ошибка: ${throwable.message}")
            }
        }

        // Задача 2: выполнится, даже если первая задача упала
        scope.launch {
            val settings = repository.fetchSettings()
            _uiState.update {
                (it as? MainUiState.Content)?.copy(settings = settings)
                    ?: MainUiState.Content(settings = settings)
            }
            viewModelScope.launch {
                _events.emit(MainEvent.ShowToast("⚡ Настройки загружены независимо!"))
            }
        }
    }

    // ============================================================
    //  🧪  ДЛЯ ОТЛАДКИ: ПРИНУДИТЕЛЬНОЕ ПЕРЕКЛЮЧЕНИЕ СОСТОЯНИЙ
    // ============================================================

    fun forceLoading() {
        _uiState.value = MainUiState.Loading
    }

    fun forceSuccess() {
        _uiState.value = MainUiState.Success(
            User(1, "Debug User", "debug@test.com")
        )
    }

    fun forceError(message: String = "Test error") {
        _uiState.value = MainUiState.Error(message)
    }

    fun forceContent() {
        viewModelScope.launch {
            repository.insertUsers(
                listOf(
                    User(name = "Test User 1", email = "test1@test.com"),
                    User(name = "Test User 2", email = "test2@test.com")
                )
            )

            // В forceContent()
            val count = repository.getCount()
            println("✅ В таблице $count записей")

            // После вставки подписка .collect { } сама обновит uiState
            _events.emit(MainEvent.ShowToast("✅ Тестовые пользователи добавлены в Room!"))
        }
    }

    // ============================================================
    //  🔍  ДЛЯ DEBOUNCE: ОБНОВЛЕНИЕ ПОИСКОВОГО ЗАПРОСА
    // ============================================================

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }
}