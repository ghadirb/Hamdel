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
    val assistantBusy by viewModel.isAssistantBusy.collectAsState()
    val simulationBusy by viewModel.isSimulationBusy.collectAsState()
    var question by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

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
                    placeholder = { Text("مثال: چطور موضوع مهمی را آرام مطرح کنم؟") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(enabled = !assistantBusy && question.isNotBlank(), onClick = { viewModel.askAssistant(question) }) {
                    Text(if (assistantBusy) "در حال پاسخ..." else "پرسیدن از دستیار")
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
                        Text("پاسخ آنلاین", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
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
                    placeholder = { Text("پیامی را که می‌خواهید پیش از ارسال بررسی کنید بنویسید.") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(enabled = !simulationBusy && message.isNotBlank(), onClick = { viewModel.simulateMessage(message) }) {
                    Text(if (simulationBusy) "در حال شبیه‌سازی..." else "شبیه‌سازی پیام")
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
                        Text("تحلیل آنلاین", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
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
