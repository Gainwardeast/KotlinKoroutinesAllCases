package com.example.kotlinkoroutinesallcases

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.kotlinkoroutinesallcases.api.ApiServiceMock
import com.example.kotlinkoroutinesallcases.dao.AppDatabase
import com.example.kotlinkoroutinesallcases.dao.UserDao
import com.example.kotlinkoroutinesallcases.repository.UserRepository
import com.example.kotlinkoroutinesallcases.view.MainViewModel
import com.example.kotlinkoroutinesallcases.view.ViewModelFactory

class MainActivity : ComponentActivity() {

    private val apiService = ApiServiceMock()
    private lateinit var database: AppDatabase
    private lateinit var userDao: UserDao
    private lateinit var repository: UserRepository
    private lateinit var viewModelFactory: ViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "user_db"
        ).build()

        userDao = database.userDao()

        repository = UserRepository(apiService, userDao)

        viewModelFactory = ViewModelFactory(repository)

        setContent {
            val viewModel: MainViewModel = viewModel(factory = viewModelFactory)
            MainScreen(viewModel = viewModel)
        }
    }
}