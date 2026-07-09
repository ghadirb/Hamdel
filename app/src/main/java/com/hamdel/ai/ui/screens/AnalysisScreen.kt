package com.hamdel.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamdel.ai.ui.RelationshipViewModel
import com.hamdel.ai.ui.components.MetricCard
import com.hamdel.ai.ui.components.MiniTrend
import com.hamdel.ai.ui.components.ScreenFrame

@Composable
fun AnalysisScreen(viewModel: RelationshipViewModel, padding: PaddingValues) {
    val state by viewModel.dashboard.collectAsState()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(bottom = 18.dp)
    ) {
        item {
            ScreenFrame("تحلیل روند رابطه", "نمودارهای احترام، صمیمیت، اختلاف، اعتماد، رضایت و احساس امنیت") {}
        }
        item {
            Card(modifier = Modifier.padding(16.dp)) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("روند ماهانه اعتماد", fontWeight = FontWeight.SemiBold)
                    MiniTrend(listOf(62, 65, 64, 69, 72, 71, 76, 78, 74, 79, 81, 83))
                    Text("اطمینان مدل: ۷۲٪، بر اساس داده‌های نمونه و گزارش‌های ذخیره‌شده.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        items(state.metrics) { MetricCard(it, Modifier.padding(horizontal = 16.dp, vertical = 5.dp).fillMaxWidth()) }
    }
}
