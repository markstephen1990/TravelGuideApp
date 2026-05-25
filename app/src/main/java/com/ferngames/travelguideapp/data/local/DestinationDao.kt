package com.ferngames.travelguideapp.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ferngames.travelguideapp.data.model.Destination

@Dao
interface DestinationDao {

    @Query("SELECT * FROM destinations")
    fun getAllDestinations(): LiveData<List<Destination>>

    @Query("SELECT * FROM destinations WHERE isWishlisted = 1")
    fun getWishlistedDestinations(): LiveData<List<Destination>>

    @Query("SELECT * FROM destinations WHERE id = :id")
    suspend fun getDestinationById(id: Int): Destination?

    @Query("SELECT * FROM destinations WHERE category = :category")
    fun getDestinationsByCategory(category: String): LiveData<List<Destination>>

    @Query("SELECT * FROM destinations WHERE name LIKE '%' || :query || '%' OR country LIKE '%' || :query || '%'")
    fun searchDestinations(query: String): LiveData<List<Destination>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDestination(destination: Destination)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(destinations: List<Destination>)

    @Update
    suspend fun updateDestination(destination: Destination)

    @Delete
    suspend fun deleteDestination(destination: Destination)

    @Query("UPDATE destinations SET isWishlisted = :isWishlisted WHERE id = :id")
    suspend fun updateWishlistStatus(id: Int, isWishlisted: Boolean)
}