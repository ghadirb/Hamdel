package com.hamdel.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamdel.ai.data.model.PersonProfile
import com.hamdel.ai.ui.RelationshipViewModel
import com.hamdel.ai.ui.components.ScreenFrame

@Composable
fun ProfileScreen(viewModel: RelationshipViewModel, padding: PaddingValues) {
    val state by viewModel.dashboard.collectAsState()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(bottom = 18.dp)
    ) {
        item {
            ScreenFrame("پروفایل و رضایت", "هر نفر حساب مستقل دارد و تحلیل مشترک فقط با رضایت هر دو نفر فعال می‌شود.") {}
        }
        items(state.profiles) { profile -> ProfileCard(profile) }
        item {
            Card(modifier = Modifier.padding(16.dp)) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("حافظه رابطه", fontWeight = FontWeight.SemiBold)
                    state.events.forEach { event ->
                        Text("${event.title}: ${event.description}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(profile: PersonProfile) {
    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(profile.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("${profile.age} ساله، ${profile.education}، ${profile.job}، ${profile.city}")
            Text("اهداف: ${profile.lifeGoals}")
            Text("ارزش‌ها: ${profile.values}")
            Text("سبک ارتباط: ${profile.communicationStyle}")
            Text("زبان عشق: ${profile.loveLanguage}")
            Text("تیپ شخصیتی: ${profile.personalityType}")
            Text("ویژگی‌ها: ${profile.traits}")
            FilterChip(
                selected = profile.consentGranted,
                onClick = {},
                label = { Text(if (profile.consentGranted) "رضایت تحلیل فعال" else "رضایت تحلیل غیرفعال") }
            )
        }
    }
}
