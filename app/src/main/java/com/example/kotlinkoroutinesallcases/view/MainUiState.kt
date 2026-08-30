package com.example.kotlinkoroutinesallcases.view

import com.example.kotlinkoroutinesallcases.data.Settings
import com.example.kotlinkoroutinesallcases.data.User


sealed class MainUiState {
    object Loading : MainUiState()
    data class Success(val user: User) : MainUiState()
    data class Error(val message: String) : MainUiState()
    data class Content(
        val user: User? = null,
        val users: List<User> = emptyList(),
        val settings: Settings? = null,
        val searchResults: List<User> = emptyList()
    ) : MainUiState()
}