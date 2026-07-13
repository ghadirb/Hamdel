package com.hamdel.ai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hamdel.ai.data.model.ConversationReport
import com.hamdel.ai.data.model.PersonProfile
import com.hamdel.ai.data.model.RelationshipEvent
import com.hamdel.ai.data.model.RelationshipMetric
import kotlinx.coroutines.flow.Flow

@Dao
interface RelationshipDao {
    @Query("SELECT * FROM relationship_metrics")
    fun observeMetrics(): Flow<List<RelationshipMetric>>

    @Query("SELECT * FROM relationship_events ORDER BY timestamp DESC")
    fun observeEvents(): Flow<List<RelationshipEvent>>

    @Query("SELECT * FROM conversation_reports ORDER BY createdAt DESC")
    fun observeReports(): Flow<List<ConversationReport>>

    @Query("SELECT * FROM profiles")
    fun observeProfiles(): Flow<List<PersonProfile>>

    @Query("SELECT COUNT(*) FROM relationship_metrics")
    suspend fun metricCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMetrics(metrics: List<RelationshipMetric>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: PersonProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfiles(profiles: List<PersonProfile>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvents(events: List<RelationshipEvent>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ConversationReport)
}
