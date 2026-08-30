package com.example.kotlinkoroutinesallcases.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.kotlinkoroutinesallcases.data.User

@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}