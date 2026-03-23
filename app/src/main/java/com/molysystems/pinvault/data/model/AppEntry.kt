package com.molysystems.pinvault.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_entries",
    indices = [Index(value = ["packageName"], unique = true)]
)
data class AppEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val displayName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis()
)
