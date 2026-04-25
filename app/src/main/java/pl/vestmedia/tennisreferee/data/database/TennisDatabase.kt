package pl.vestmedia.tennisreferee.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room Database dla aplikacji
 */
@Database(
    entities = [MatchEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TennisDatabase : RoomDatabase() {
    
    abstract fun matchDao(): MatchDao
    
    companion object {
        @Volatile
        private var INSTANCE: TennisDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE matches ADD COLUMN player3 TEXT")
                db.execSQL("ALTER TABLE matches ADD COLUMN player4 TEXT")
                db.execSQL("ALTER TABLE matches ADD COLUMN isDoubles INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE matches ADD COLUMN isMixedDoubles INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE matches ADD COLUMN umpireName TEXT")
            }
        }
        
        fun getDatabase(context: Context): TennisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TennisDatabase::class.java,
                    "tennis_referee_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
