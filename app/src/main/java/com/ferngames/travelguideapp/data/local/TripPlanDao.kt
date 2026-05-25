package com.ferngames.travelguideapp.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ferngames.travelguideapp.data.model.TripPlan

@Dao
interface TripPlanDao {

    @Query("SELECT * FROM trip_plans ORDER BY createdAt DESC")
    fun getAllTripPlans(): LiveData<List<TripPlan>>

    @Query("SELECT * FROM trip_plans WHERE id = :id")
    suspend fun getTripPlanById(id: Int): TripPlan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTripPlan(tripPlan: TripPlan)

    @Update
    suspend fun updateTripPlan(tripPlan: TripPlan)

    @Delete
    suspend fun deleteTripPlan(tripPlan: TripPlan)
}