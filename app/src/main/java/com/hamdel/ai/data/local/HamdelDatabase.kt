package com.hamdel.ai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 1,
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
                ).build().also { instance = it }
            }
        }
    }
}
