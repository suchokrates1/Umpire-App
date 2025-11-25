package pl.vestmedia.tennisreferee.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room Database dla aplikacji
 */
@Database(
    entities = [MatchEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TennisDatabase : RoomDatabase() {
    
    abstract fun matchDao(): MatchDao
    
    companion object {
        @Volatile
        private var INSTANCE: TennisDatabase? = null
        
        fun getDatabase(context: Context): TennisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TennisDatabase::class.java,
                    "tennis_referee_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
