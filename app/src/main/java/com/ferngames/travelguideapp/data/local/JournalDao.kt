package com.ferngames.travelguideapp.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ferngames.travelguideapp.data.model.JournalEntry

@Dao
interface JournalDao {

    @Query("SELECT * FROM journal_entries ORDER BY createdAt DESC")
    fun getAllJournalEntries(): LiveData<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE destinationName = :destination")
    fun getEntriesForDestination(destination: String): LiveData<List<JournalEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntry)

    @Update
    suspend fun updateEntry(entry: JournalEntry)

    @Delete
    suspend fun deleteEntry(entry: JournalEntry)
}