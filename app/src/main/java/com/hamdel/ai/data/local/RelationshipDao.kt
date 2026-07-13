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

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun profileCount(): Int

    @Query("SELECT COUNT(*) FROM profiles WHERE id IN ('person_a', 'person_b')")
    suspend fun legacyProfileCount(): Int

    @Query("SELECT COUNT(*) FROM conversation_reports")
    suspend fun reportCount(): Int

    @Query("DELETE FROM relationship_metrics")
    suspend fun clearMetrics()

    @Query("DELETE FROM profiles WHERE id IN ('person_a', 'person_b')")
    suspend fun clearLegacyProfiles()

    @Query("DELETE FROM relationship_events WHERE id IN ('first_meet', 'important_talk', 'repair')")
    suspend fun clearLegacyEvents()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMetrics(metrics: List<RelationshipMetric>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: PersonProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvents(events: List<RelationshipEvent>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ConversationReport)
}
