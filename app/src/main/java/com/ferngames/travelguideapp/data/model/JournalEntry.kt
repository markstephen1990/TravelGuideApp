package com.ferngames.travelguideapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val destinationName: String,
    val title: String,
    val content: String,
    val rating: Float,
    val date: String,
    val createdAt: Long = System.currentTimeMillis()
)