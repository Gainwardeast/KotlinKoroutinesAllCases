package com.example.kotlinkoroutinesallcases

import android.widget.Toast
import androidx.compose.foundation.layout.*
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.onSearchQueryChanged(it)
                },
                label = { Text("Search (min 3 chars)") },
                modifier = Modifier.fillMaxWidth()
            )

            when (val state = uiState) {
                is MainUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is MainUiState.Success -> {
                    Text("User: ${state.user.name}")
                    Button(onClick = { viewModel.showTemporaryMessage() }) {
                        Text("Show temp message")
                    }
                    Button(onClick = { viewModel.riskyOperation() }) {
                        Text("Risky operation")
                    }
                }
                is MainUiState.Error -> {
                    Text("Error: ${state.message}")
                    Button(onClick = { viewModel.loadUserWithCancel() }) {
                        Text("Retry")
                    }
                }
                is MainUiState.Content -> {
                    Text("Content screen")
                    Button(onClick = { viewModel.loadIndependentData() }) {
                        Text("Load independent data")
                    }
                }
            }
        }
    }
}