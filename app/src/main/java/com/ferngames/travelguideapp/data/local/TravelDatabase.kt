package com.ferngames.travelguideapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ferngames.travelguideapp.data.model.Destination
import com.ferngames.travelguideapp.data.model.JournalEntry
import com.ferngames.travelguideapp.data.model.TripPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Destination::class, TripPlan::class, JournalEntry::class],
    version = 1,
    exportSchema = false
)
abstract class TravelDatabase : RoomDatabase() {

    abstract fun destinationDao(): DestinationDao
    abstract fun tripPlanDao(): TripPlanDao
    abstract fun journalDao(): JournalDao

    companion object {
        @Volatile
        private var INSTANCE: TravelDatabase? = null

        fun getDatabase(context: Context): TravelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TravelDatabase::class.java,
                    "travel_database"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateDatabase(database.destinationDao())
                }
            }
        }

        suspend fun populateDatabase(dao: DestinationDao) {
            val destinations = listOf(
                Destination(
                    name = "Bali",
                    country = "Indonesia",
                    description = "A tropical paradise known for its forested volcanic mountains, iconic rice paddies, beaches and coral reefs.",
                    imageUrl = "https://images.unsplash.com/photo-1537996194471-e657df975ab4?w=800",
                    category = "Beach",
                    rating = 4.8,
                    latitude = -8.3405,
                    longitude = 115.0920,
                    bestTimeToVisit = "April to October",
                    currency = "IDR",
                    language = "Balinese, Indonesian"
                ),
                Destination(
                    name = "Paris",
                    country = "France",
                    description = "The City of Light dazzles with its iconic Eiffel Tower, world-class museums, and exquisite cuisine.",
                    imageUrl = "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=800",
                    category = "City",
                    rating = 4.9,
                    latitude = 48.8566,
                    longitude = 2.3522,
                    bestTimeToVisit = "June to August",
                    currency = "EUR",
                    language = "French"
                ),
                Destination(
                    name = "Santorini",
                    country = "Greece",
                    description = "Famous for its dramatic views, stunning sunsets, and white-washed buildings with blue domes.",
                    imageUrl = "https://images.unsplash.com/photo-1570077188670-e3a8d69ac5ff?w=800",
                    category = "Beach",
                    rating = 4.7,
                    latitude = 36.3932,
                    longitude = 25.4615,
                    bestTimeToVisit = "May to September",
                    currency = "EUR",
                    language = "Greek"
                ),
                Destination(
                    name = "Tokyo",
                    country = "Japan",
                    description = "A city where ultra-modern and traditional come together, from neon-lit skyscrapers to historic temples.",
                    imageUrl = "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800",
                    category = "City",
                    rating = 4.9,
                    latitude = 35.6762,
                    longitude = 139.6503,
                    bestTimeToVisit = "March to May",
                    currency = "JPY",
                    language = "Japanese"
                ),
                Destination(
                    name = "Machu Picchu",
                    country = "Peru",
                    description = "An ancient Incan citadel set high in the Andes Mountains, a UNESCO World Heritage Site.",
                    imageUrl = "https://images.unsplash.com/photo-1526392060635-9d6019884377?w=800",
                    category = "Adventure",
                    rating = 4.9,
                    latitude = -13.1631,
                    longitude = -72.5450,
                    bestTimeToVisit = "May to September",
                    currency = "PEN",
                    language = "Spanish"
                ),
                Destination(
                    name = "Maldives",
                    country = "Maldives",
                    description = "A tropical nation known for its turquoise waters, overwater bungalows, and vibrant coral reefs.",
                    imageUrl = "https://images.unsplash.com/photo-1573843981267-be1999ff37cd?w=800",
                    category = "Beach",
                    rating = 4.8,
                    latitude = 3.2028,
                    longitude = 73.2207,
                    bestTimeToVisit = "November to April",
                    currency = "MVR",
                    language = "Dhivehi"
                ),
                Destination(
                    name = "New York",
                    country = "USA",
                    description = "The city that never sleeps, home to Times Square, Central Park, and world-famous landmarks.",
                    imageUrl = "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800",
                    category = "City",
                    rating = 4.7,
                    latitude = 40.7128,
                    longitude = -74.0060,
                    bestTimeToVisit = "April to June",
                    currency = "USD",
                    language = "English"
                ),
                Destination(
                    name = "Safari Kenya",
                    country = "Kenya",
                    description = "Experience the world's greatest wildlife spectacle with the Great Migration in the Masai Mara.",
                    imageUrl = "https://images.unsplash.com/photo-1516426122078-c23e76319801?w=800",
                    category = "Adventure",
                    rating = 4.8,
                    latitude = -1.2921,
                    longitude = 36.8219,
                    bestTimeToVisit = "July to October",
                    currency = "KES",
                    language = "Swahili, English"
                )
            )
            dao.insertAll(destinations)
        }
    }
}