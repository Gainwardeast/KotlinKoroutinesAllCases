# KotlinKoroutinesAllCases

Учебное Android-приложение: справочник по **все основным сценариям работы с Kotlin Coroutines и Flow** на практике. Каждый сценарий оформлен как работающий кейс с кнопкой в UI (Jetpack Compose), логами и комментариями в коде — удобно как шпаргалка и как основа для интервью-подготовки.

## 🧪 Покрытые сценарии

Все кейсы реализованы в [`MainViewModel.kt`](app/src/main/java/com/example/kotlinkoroutinesallcases/view/MainViewModel.kt):

| # | Сценарий | Инструменты |
|---|---|---|
| 1 | Фоновая работа | `withContext`, `Dispatchers.IO`, обработка ошибок через `try-catch` |
| 2 | Реактивная подписка | `collect`, `StateFlow`, обновление UI из Flow базы данных |
| 3 | Параллельный запуск задач | `async` / `await` внутри `coroutineScope` |
| 4 | Отмена задачи | `Job.cancel()`, управление жизненным циклом загрузки |
| 5 | Задержки | `delay` |
| 6 | Обработка ошибок | `try-catch` + `CoroutineExceptionHandler` |
| 6.3 | Изоляция ошибок | `SupervisorJob` — падение одной задачи не убивает соседние |

## 🔎 Flow-операторы в действии

Реальный пример поисковой строки с асинхронным запросом:

```kotlin
searchQuery
    .debounce(500)             // ждём паузы в вводе
    .distinctUntilChanged()    // игнорируем повторы
    .filter { it.length >= 3 } // короткие запросы не ищем
    .mapLatest { query ->      // отменяем устаревший запрос
        repository.searchUsers(query)
    }
    .collect { results -> ... }
```

Также в проекте показаны:

- `stateIn(..., SharingStarted.WhileSubscribed(5000), ...)` — превращение холодного Flow из Room в горячее состояние для UI
- `MutableSharedFlow` — одноразовые события (тосты) отдельно от состояния
- `_uiState.update { }` — атомарное обновление состояния

## 🛠 Технологии

| Категория | Технологии |
|---|---|
| Язык | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Асинхронность | Kotlinx Coroutines 1.7.3 + Flow |
| Архитектура | MVVM (UiState / Events), Repository |
| Кэш | Room 2.6 (kapt) |

## 🏗 Структура

```
app/src/main/java/com/example/kotlinkoroutinesallcases/
├── view/            # MainViewModel (все сценарии), UiState, Events, Factory
├── repository/      # UserRepository (эмуляция сети + Room)
├── dao/             # Room: AppDatabase, UserDao
├── api/             # ApiService
├── data/            # модели User, Settings
└── MainScreen.kt    # Compose-UI с кнопками для каждого сценария
```

## 🚀 Сборка и запуск

**Требования:**
- Android Studio
- JDK 17
- Минимальная версия Android: **7.0 (API 24)**, целевая: **14 (API 34)**

```bash
git clone https://github.com/Gainwardeast/KotlinKoroutinesAllCases.git
cd KotlinKoroutinesAllCases
./gradlew installDebug   # или открыть в Android Studio и нажать Run
```

На главном экране — кнопки для каждого сценария; ход выполнения виден в Logcat (метки `✅ COLLECT:`, `✅ В таблице N записей` и др.).

## 🗺 Планы

- [ ] Сценарии с `Flow` операторами: `combine`, `zip`, `flatMapLatest`, `retryWhen`
- [ ] Демо `Channel` и `select`
- [ ] Тестирование корутин: `runTest`, `TestDispatcher`, `Turbine`
- [ ] Заменить kapt на KSP для Room

## 📄 Лицензия

Пока не определена.
