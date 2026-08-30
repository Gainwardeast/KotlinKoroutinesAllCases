package com.example.kotlinkoroutinesallcases

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.kotlinkoroutinesallcases.api.ApiServiceMock
import com.example.kotlinkoroutinesallcases.dao.AppDatabase
import com.example.kotlinkoroutinesallcases.repository.UserRepository
import com.example.kotlinkoroutinesallcases.view.MainViewModel
import com.example.kotlinkoroutinesallcases.view.ViewModelFactory

class MainActivity : ComponentActivity() {

    private val apiService = ApiServiceMock()
    private val database = Room.databaseBuilder(
        applicationContext,
        AppDatabase::class.java,
        "user_db"
    ).build()
    private val userDao = database.userDao()
    private val repository = UserRepository(apiService, userDao)
    private val viewModelFactory = ViewModelFactory(repository)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = viewModel(factory = viewModelFactory)
            MainScreen(viewModel = viewModel)
        }
    }
}