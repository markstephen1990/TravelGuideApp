package com.ferngames.travelguideapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "destinations")
data class Destination(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val country: String,
    val description: String,
    val imageUrl: String,
    val category: String,
    val rating: Double,
    val latitude: Double,
    val longitude: Double,
    val bestTimeToVisit: String,
    val currency: String,
    val language: String,
    val isWishlisted: Boolean = false
)