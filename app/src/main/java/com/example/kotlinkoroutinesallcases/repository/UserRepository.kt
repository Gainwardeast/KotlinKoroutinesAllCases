package com.example.kotlinkoroutinesallcases.repository

import com.example.kotlinkoroutinesallcases.api.ApiService
import com.example.kotlinkoroutinesallcases.dao.UserDao
import com.example.kotlinkoroutinesallcases.data.Settings
import com.example.kotlinkoroutinesallcases.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class UserRepository(
    private val api: ApiService,
    private val dao: UserDao
) {

    private var cachedUser: User? = null

    suspend fun fetchSettings(): Settings {
        return withContext(Dispatchers.IO) {
            api.fetchSettings()
        }
    }

    // UserRepository.kt
    suspend fun insertUsers(users: List<User>) {
        withContext(Dispatchers.IO) {
            users.forEach { dao.insert(it) }
        }
    }

    suspend fun getUser(id: Int): User {
        cachedUser?.let {
            if (it.id == id) return it
        }

        return withContext(Dispatchers.IO) {
            val user = api.fetchUser(id)
            cachedUser = user
            dao.insert(user)
            user
        }
    }

    fun getUsersFlow(): Flow<List<User>> = dao.getAllUsers()
        .catch { emit(emptyList()) }

    suspend fun searchUsers(query: String): List<User> {
        return withContext(Dispatchers.IO) {
            // Эмуляция поиска
            listOf(User(1, "Found: $query", "found@example.com"))
        }
    }

    suspend fun getCount(): Int {
        return withContext(Dispatchers.IO) {
            dao.getCount()
        }
    }
}