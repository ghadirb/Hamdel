package com.hamdel.ai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hamdel.ai.data.model.ConversationReport
import com.hamdel.ai.data.model.PersonProfile
import com.hamdel.ai.data.model.RelationshipEvent
import com.hamdel.ai.data.model.RelationshipMetric

@Database(
    entities = [
        PersonProfile::class,
        RelationshipMetric::class,
        RelationshipEvent::class,
        ConversationReport::class
    ],
    version = 2,
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
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE conversation_reports ADD COLUMN transcript TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
