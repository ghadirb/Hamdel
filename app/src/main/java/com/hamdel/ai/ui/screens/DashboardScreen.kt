package com.hamdel.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamdel.ai.ui.RelationshipViewModel
import com.hamdel.ai.ui.components.MetricCard
import com.hamdel.ai.ui.components.MiniTrend
import com.hamdel.ai.ui.components.ScreenFrame

@Composable
fun DashboardScreen(viewModel: RelationshipViewModel, padding: PaddingValues) {
    val state by viewModel.dashboard.collectAsState()
    var showConsentInfo by remember { mutableStateOf(false) }
    var showMemory by remember { mutableStateOf(false) }
    val consentText = when {
        state.profiles.size < 2 -> "برای فعال‌شدن تحلیل، دو پروفایل بسازید."
        state.profiles.all { it.consentGranted } -> "رضایت هر دو نفر فعال است."
        else -> "رضایت هر دو نفر هنوز فعال نیست."
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(bottom = 18.dp)
    ) {
        item {
            ScreenFrame(
                title = "همدل",
                subtitle = "خلاصه وضعیت رابطه، هشدارها و پیشنهادهای امروز"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        AssistChip(onClick = { showConsentInfo = !showConsentInfo }, label = { Text("رضایت دوطرفه") })
                        AssistChip(onClick = { showMemory = !showMemory }, label = { Text("حافظه رابطه") })
                    }
                    if (showConsentInfo) {
                        Text(consentText, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (state.metrics.isEmpty()) {
            item {
                Text(
                    "پس از تحلیل اولین گفتگو یا جلسه، شاخص‌های رابطه اینجا نمایش داده می‌شوند.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(state.metrics) { metric ->
                MetricCard(metric = metric, modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp))
            }
        }
        item {
            Card(
                modifier = Modifier.padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("روند رابطه", fontWeight = FontWeight.SemiBold)
                    if (state.metrics.size >= 2) {
                        MiniTrend(state.metrics.map { it.value })
                    } else {
                        Text("داده کافی برای نمایش روند وجود ندارد.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            InfoSection("آخرین هشدارها", state.warnings)
            InfoSection("پیشنهادهای امروز", state.suggestions)
        }
        if (showMemory) {
            item {
                Card(modifier = Modifier.padding(16.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("حافظه رابطه", fontWeight = FontWeight.SemiBold)
                        if (state.events.isEmpty()) {
                            Text("هنوز رویداد یا تحلیلی در حافظه رابطه ثبت نشده است.")
                        } else {
                            state.events.take(10).forEach { event ->
                                Text(event.title, fontWeight = FontWeight.Medium)
                                Text(event.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoSection(title: String, items: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, fontWeight = FontWeight.SemiBold)
            if (items.isEmpty()) {
                Text("هنوز داده‌ای برای نمایش وجود ندارد.", style = MaterialTheme.typography.bodyMedium)
            } else {
                items.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}
