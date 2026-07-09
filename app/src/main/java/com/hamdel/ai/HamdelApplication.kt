package com.hamdel.ai

import android.app.Application
import com.hamdel.ai.data.local.HamdelDatabase
import com.hamdel.ai.data.repository.RelationshipRepository
import com.hamdel.ai.domain.DemoRelationshipAiEngine

class HamdelApplication : Application() {
    val database by lazy { HamdelDatabase.getDatabase(this) }
    val repository by lazy {
        RelationshipRepository(
            database.relationshipDao(),
            DemoRelationshipAiEngine()
        )
    }
}
