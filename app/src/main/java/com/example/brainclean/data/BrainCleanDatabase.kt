package com.example.brainclean.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ThoughtEntity::class],
    version = 5,
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
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE thoughts ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE thoughts ADD COLUMN completedAt INTEGER")
                database.execSQL(
                    """
                    UPDATE thoughts
                    SET createdAt = id,
                        completedAt = CASE WHEN status = 'DONE' THEN id ELSE NULL END
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE thoughts ADD COLUMN inboxEnteredAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE thoughts ADD COLUMN staleInboxReminderSentAt INTEGER")
                database.execSQL(
                    """
                    UPDATE thoughts
                    SET inboxEnteredAt = CASE
                        WHEN status = 'INBOX' THEN createdAt
                        ELSE 0
                    END
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE thoughts ADD COLUMN captureSource TEXT NOT NULL DEFAULT 'APP'"
                )
            }
        }
    }
}
