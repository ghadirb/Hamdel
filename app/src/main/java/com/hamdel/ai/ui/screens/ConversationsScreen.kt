package com.hamdel.ai.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamdel.ai.data.model.ConversationReport
import com.hamdel.ai.ui.RelationshipViewModel
import com.hamdel.ai.ui.components.ScreenFrame

@Composable
fun ConversationsScreen(viewModel: RelationshipViewModel, padding: PaddingValues) {
    val context = LocalContext.current
    val state by viewModel.dashboard.collectAsState()
    val status by viewModel.statusMessage.collectAsState()
    val busy by viewModel.isBusy.collectAsState()
    var title by remember { mutableStateOf("Export چت") }
    var text by remember { mutableStateOf("") }

    val textFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            text = context.contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()
            title = uri.lastPathSegment?.substringAfterLast('/') ?: "فایل گفتگو"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(bottom = 18.dp)
    ) {
        item {
            ScreenFrame("تحلیل گفتگوها", "متن گفتگو را بچسبانید یا فایل export چت را انتخاب کنید؛ تحلیل با GapGPT و در صورت خطا با Liara انجام می‌شود.") {
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
                    minLines = 7,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { textFileLauncher.launch("text/*") }) {
                        Text("انتخاب فایل")
                    }
                    Button(
                        enabled = !busy && text.isNotBlank(),
                        onClick = { viewModel.analyzeConversation(title, text) }
                    ) {
                        Text(if (busy) "در حال تحلیل..." else "تحلیل گفتگو")
                    }
                }
                status?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
