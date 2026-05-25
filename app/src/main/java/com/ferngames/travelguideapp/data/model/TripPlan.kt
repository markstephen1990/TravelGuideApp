package com.ferngames.travelguideapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_plans")
data class TripPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val destinationName: String,
    val startDate: String,
    val endDate: String,
    val notes: String,
    val budget: Double,
    val createdAt: Long = System.currentTimeMillis()
)