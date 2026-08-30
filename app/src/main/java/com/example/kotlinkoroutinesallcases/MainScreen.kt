package com.example.kotlinkoroutinesallcases

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotlinkoroutinesallcases.view.MainEvent
import com.example.kotlinkoroutinesallcases.view.MainUiState
import com.example.kotlinkoroutinesallcases.view.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }

    // Обработка событий (Toast)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MainEvent.ShowToast -> {
                    Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
                }
                is MainEvent.HideToast -> {
                    // Можно скрыть тост
                }
                is MainEvent.NavigateToProfile -> {
                    // Навигация
                }
            }
        }
    }

    // Автозагрузка при старте
    LaunchedEffect(Unit) {
        viewModel.loadUserWithCancel()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ========== ПОЛЕ ПОИСКА (debounce) ==========
            Text(
                text = "🔍 Debounce (сценарий 5)",
                style = MaterialTheme.typography.titleSmall
            )
            TextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.onSearchQueryChanged(it)
                },
                label = { Text("Search (min 3 chars)") },
                modifier = Modifier.fillMaxWidth()
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // ========== ОСНОВНЫЕ СЦЕНАРИИ (кнопки) ==========
            Text(
                text = "🚀 Сценарии корутин",
                style = MaterialTheme.typography.titleMedium
            )

            // 1️⃣ Сценарий 1: Фоновая работа (withContext)
            Button(
                onClick = { viewModel.loadUserWithCancel() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("1️⃣ Загрузить пользователя (withContext)")
            }

            // 2️⃣ Сценарий 3: Параллельный запуск (async)
            Button(
                onClick = { viewModel.loadParallel() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("3️⃣ Параллельные запросы (async/await)")
            }

            // 3️⃣ Сценарий 4: Отмена задачи (job.cancel)
            Button(
                onClick = { viewModel.cancelLoading() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("4️⃣ Отменить загрузку (job.cancel)")
            }

            // 4️⃣ Сценарий 5: Задержка (delay)
            Button(
                onClick = { viewModel.showTemporaryMessage() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("5️⃣ Задержка (delay + Toast)")
            }

            // 5️⃣ Сценарий 6: Обработка ошибок (try-catch)
            Button(
                onClick = { viewModel.riskyOperation() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("6️⃣ Ошибка (try-catch / CoroutineExceptionHandler)")
            }

            // 6️⃣ Сценарий 6.3: SupervisorJob
            Button(
                onClick = { viewModel.loadIndependentData() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("6️⃣ SupervisorJob (ошибка не убивает соседей)")
            }

            // 7️⃣ Сценарий 2: Реактивная подписка (collect)
            Button(
                onClick = { viewModel.forceContent() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("2️⃣ Показать Flow (collect из Room)")
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // ========== ОТОБРАЖЕНИЕ ТЕКУЩЕГО СОСТОЯНИЯ ==========
            Text(
                text = "📊 Текущее состояние:",
                style = MaterialTheme.typography.titleSmall
            )

            when (val state = uiState) {
                is MainUiState.Loading -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Загрузка...")
                        }
                    }
                }

                is MainUiState.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("✅ Успешно загружен пользователь:")
                            Text("• Имя: ${state.user.name}")
                            Text("• Email: ${state.user.email}")
                        }
                    }
                }

                is MainUiState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("❌ Ошибка:", color = MaterialTheme.colorScheme.error)
                            Text("${state.message}")
                        }
                    }
                }

                is MainUiState.Content -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("📄 Состояние Content:")

                            if (state.users.isNotEmpty()) {
                                Text("• Пользователи из БД: ${state.users.size}")
                            }

                            if (state.searchResults.isNotEmpty()) {
                                Text("• Результаты поиска: ${state.searchResults.size}")
                                state.searchResults.forEach { user ->
                                    Text("  - ${user.name} (${user.email})")
                                }
                            }

                            if (state.settings != null) {
                                Text("• Настройки: тема=${state.settings.isDarkMode}, язык=${state.settings.language}")
                            }

                            if (state.users.isEmpty() && state.searchResults.isEmpty() && state.settings == null) {
                                Text("(пусто)")
                            }
                        }
                    }
                }
            }
        }
    }
}