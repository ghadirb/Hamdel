package com.hamdel.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hamdel.ai.ui.HamdelApp
import com.hamdel.ai.ui.RelationshipViewModel
import com.hamdel.ai.ui.theme.HamdelTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<RelationshipViewModel> {
        val app = application as HamdelApplication
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RelationshipViewModel(
                    repository = app.repository,
                    audioClient = app.audioClient,
                    startupMessageClient = app.startupMessageClient,
                    appContext = app.applicationContext,
                    contactSmsImporter = app.contactSmsImporter
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HamdelTheme {
                HamdelApp(viewModel)
            }
        }
    }
}
