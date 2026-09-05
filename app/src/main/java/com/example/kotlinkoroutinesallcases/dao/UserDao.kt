package com.example.kotlinkoroutinesallcases.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kotlinkoroutinesallcases.data.User
import kotlinx.coroutines.flow.Flow

@Dao
interface  UserDao {
    @Insert()
    suspend fun insert(user: User)

    @Query("SELECT * FROM user")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT COUNT(*) FROM user")
    suspend fun getCount(): Int  // ← Добавь этот метод
}