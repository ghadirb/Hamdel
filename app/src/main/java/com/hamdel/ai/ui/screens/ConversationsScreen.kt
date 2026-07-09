package com.hamdel.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
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
import com.hamdel.ai.data.model.ConversationReport
import com.hamdel.ai.ui.RelationshipViewModel
import com.hamdel.ai.ui.components.ScreenFrame

@Composable
fun ConversationsScreen(viewModel: RelationshipViewModel, padding: PaddingValues) {
    val state by viewModel.dashboard.collectAsState()
    var title by remember { mutableStateOf("Export چت") }
    var text by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(bottom = 18.dp)
    ) {
        item {
            ScreenFrame("تحلیل گفتگوها", "متن گفتگو را بچسبانید یا بعدا فایل/export چت را به همین جریان وصل کنید.") {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان منبع") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("متن گفتگو") },
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = { viewModel.analyzeConversation(title, text) }) {
                    Text("تحلیل گفتگو")
                }
            }
        }
        items(state.reports) { report -> ConversationReportCard(report) }
    }
}

@Composable
private fun ConversationReportCard(report: ConversationReport) {
    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(report.sourceTitle, fontWeight = FontWeight.SemiBold)
            Text(report.summary, style = MaterialTheme.typography.bodyMedium)
            ScoreBar("احترام", report.respect)
            ScoreBar("همدلی", report.empathy)
            ScoreBar("صداقت", report.honesty)
            ScoreBar("ریسک طعنه", report.sarcasmRisk)
            ScoreBar("ریسک کنترل‌گری", report.controlRisk)
            ScoreBar("حمایت عاطفی", report.emotionalSupport)
        }
    }
}

@Composable
fun ScoreBar(label: String, value: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$label: $value", style = MaterialTheme.typography.labelLarge)
        LinearProgressIndicator(progress = value / 100f, modifier = Modifier.fillMaxWidth())
    }
}
