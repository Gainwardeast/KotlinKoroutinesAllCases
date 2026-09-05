package com.example.kotlinkoroutinesallcases.api

import com.example.kotlinkoroutinesallcases.data.Settings
import com.example.kotlinkoroutinesallcases.data.User
import kotlinx.coroutines.delay
import java.io.IOException

interface ApiService {
    suspend fun fetchUser(id: Int): User
    suspend fun fetchSettings(): Settings
}

class ApiServiceMock : ApiService {
    override suspend fun fetchUser(id: Int): User {
        delay(1000)
        if (id <= 0) throw IOException("User not found (id <= 0)")  // ✅ Падает при id <= 0
        return User(id, "John Doe", "john@example.com")
    }

    override suspend fun fetchSettings(): Settings {
        delay(800)
        return Settings(true, "ru")
    }
}