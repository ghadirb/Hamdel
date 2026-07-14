package com.hamdel.ai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hamdel.ai.data.model.ConversationReport
import com.hamdel.ai.data.model.ContactMessage
import com.hamdel.ai.data.model.ProfileSuggestion
import com.hamdel.ai.data.model.PersonProfile
import com.hamdel.ai.data.model.RelationshipEvent
import com.hamdel.ai.data.model.RelationshipMetric

@Database(
    entities = [
        PersonProfile::class,
        RelationshipMetric::class,
        RelationshipEvent::class,
        ConversationReport::class,
        ContactMessage::class,
        ProfileSuggestion::class
    ],
    version = 4,
    exportSchema = false
)
abstract class HamdelDatabase : RoomDatabase() {
    abstract fun relationshipDao(): RelationshipDao

    companion object {
        @Volatile
        private var instance: HamdelDatabase? = null

        fun getDatabase(context: Context): HamdelDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HamdelDatabase::class.java,
                    "hamdel.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE conversation_reports ADD COLUMN transcript TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS contact_messages (" +
                        "id TEXT NOT NULL PRIMARY KEY, contactName TEXT NOT NULL, address TEXT NOT NULL, " +
                        "body TEXT NOT NULL, timestamp INTEGER NOT NULL, direction TEXT NOT NULL)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS profile_suggestions (" +
                        "id TEXT NOT NULL PRIMARY KEY, profileId TEXT NOT NULL, profileName TEXT NOT NULL, " +
                        "field TEXT NOT NULL, proposedValue TEXT NOT NULL, reason TEXT NOT NULL, " +
                        "confidence REAL NOT NULL, createdAt INTEGER NOT NULL)"
                )
            }
        }

        fun closeDatabase() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }
    }
}
