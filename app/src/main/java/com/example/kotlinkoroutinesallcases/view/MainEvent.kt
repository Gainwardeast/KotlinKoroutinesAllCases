package com.example.kotlinkoroutinesallcases.view

sealed class MainEvent {
    data class ShowToast(val text: String) : MainEvent()
    object HideToast : MainEvent()
    data class NavigateToProfile(val userId: Int) : MainEvent()
}