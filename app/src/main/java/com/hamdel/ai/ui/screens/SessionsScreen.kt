package com.hamdel.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamdel.ai.ui.RelationshipViewModel
import com.hamdel.ai.ui.components.ScreenFrame

@Composable
fun SessionsScreen(viewModel: RelationshipViewModel, padding: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(bottom = 18.dp)
    ) {
        item {
            ScreenFrame("جلسات گفتگو", "مسیر ضبط، تبدیل گفتار به متن، تفکیک گوینده و خلاصه جلسه برای نسخه تولیدی آماده شده است.") {
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {}) {
                        Icon(Icons.Outlined.Mic, contentDescription = null)
                        Text("شروع ضبط")
                    }
                    Button(onClick = {}) {
                        Icon(Icons.Outlined.Stop, contentDescription = null)
                        Text("پایان")
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.padding(16.dp)) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("نمونه خلاصه جلسه", fontWeight = FontWeight.SemiBold)
                    Text("زمان صحبت نفر اول: ۵۴٪")
                    Text("زمان صحبت نفر دوم: ۴۶٪")
                    Text("موضوع اختلاف: زمان‌بندی دیدارها و نحوه بیان ناراحتی")
                    Text("لحن کلی: آرام با چند نقطه تنش کوتاه", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
