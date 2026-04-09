package com.example.brainclean.data

import android.content.Context
import androidx.room.Room
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ThoughtEntity::class],
    version = 2,
    exportSchema = false
)
abstract class BrainCleanDatabase : RoomDatabase() {
    abstract fun thoughtDao(): ThoughtDao

    companion object {
        @Volatile
        private var INSTANCE: BrainCleanDatabase? = null

        fun getDatabase(context: Context): BrainCleanDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    BrainCleanDatabase::class.java,
                    "brain_clean_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { database ->
                    INSTANCE = database
                }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE thoughts ADD COLUMN remindAt INTEGER")
            }
        }
    }
}
