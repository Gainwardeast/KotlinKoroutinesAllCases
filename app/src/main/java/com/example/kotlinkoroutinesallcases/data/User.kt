package com.example.kotlinkoroutinesallcases.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class User(
    @PrimaryKey val id: Int,
    val name: String,
    val email: String
)