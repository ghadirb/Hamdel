package com.hamdel.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.hamdel.ai.ui.components.ScreenFrame

@Composable
fun AssistantScreen(viewModel: RelationshipViewModel, padding: PaddingValues) {
    val reply by viewModel.assistantReply.collectAsState()
    val simulation by viewModel.simulation.collectAsState()
    val busy by viewModel.isBusy.collectAsState()
    var question by remember { mutableStateOf("آیا الان زمان مناسبی برای ازدواج است؟") }
    var message by remember { mutableStateOf("تو همیشه حرف من را نمی‌فهمی و باید تغییر کنی.") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(bottom = 18.dp)
    ) {
        item {
            ScreenFrame("دستیار هوشمند", "پاسخ‌ها با حافظه رابطه، پروفایل‌ها و تحلیل‌های قبلی شخصی‌سازی می‌شوند.") {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("سوال") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(enabled = !busy && question.isNotBlank(), onClick = { viewModel.askAssistant(question) }) {
                    Text(if (busy) "در حال پاسخ..." else "پرسیدن از دستیار")
                }
            }
        }
        item {
            reply?.let {
                Card(modifier = Modifier.padding(16.dp)) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("پاسخ دستیار", fontWeight = FontWeight.SemiBold)
                        Text(it.answer)
                        Text("اطمینان: ${(it.confidence * 100).toInt()}٪", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("دلایل: ${it.reasons.joinToString("، ")}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            ScreenFrame("شبیه‌ساز پیام", "قبل از ارسال پیام، ریسک ناراحت شدن، سوءتفاهم و نسخه بهتر پیام را ببینید.") {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("متن پیام") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(enabled = !busy && message.isNotBlank(), onClick = { viewModel.simulateMessage(message) }) {
                    Text(if (busy) "در حال شبیه‌سازی..." else "شبیه‌سازی")
                }
            }
        }
        item {
            simulation?.let {
                Card(modifier = Modifier.padding(16.dp)) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("نتیجه شبیه‌سازی", fontWeight = FontWeight.SemiBold)
                        ScoreBar("احتمال دعوا", it.conflictRisk)
                        ScoreBar("احتمال سوءتفاهم", it.misunderstandingRisk)
                        ScoreBar("احتمال ناراحت شدن", it.hurtRisk)
                        Text("نسخه بهتر:", fontWeight = FontWeight.SemiBold)
                        Text(it.improvedMessage)
                        it.notes.forEach { note -> Text("• $note", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}
