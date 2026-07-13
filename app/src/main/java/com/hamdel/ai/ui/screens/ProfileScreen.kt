package com.hamdel.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hamdel.ai.data.model.PersonProfile
import com.hamdel.ai.ui.RelationshipViewModel
import com.hamdel.ai.ui.components.ScreenFrame
import java.util.UUID

@Composable
fun ProfileScreen(viewModel: RelationshipViewModel, padding: PaddingValues) {
    val state by viewModel.dashboard.collectAsState()
    val status by viewModel.statusMessage.collectAsState()
    var editing by remember { mutableStateOf<PersonProfile?>(null) }
    var showNewForm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(bottom = 18.dp)
    ) {
        item {
            ScreenFrame("پروفایل و رضایت", "پروفایل باید توسط خود افراد قابل ایجاد و ویرایش باشد؛ برنامه فقط تحلیل می‌کند و بدون رضایت دوطرفه تحلیل مشترک انجام نمی‌دهد.") {
                Button(onClick = {
                    editing = null
                    showNewForm = true
                }) {
                    Text("افزودن پروفایل")
                }
                status?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        if (showNewForm || editing != null) {
            item {
                ProfileEditor(
                    initial = editing,
                    onCancel = {
                        editing = null
                        showNewForm = false
                    },
                    onSave = {
                        viewModel.saveProfile(it)
                        editing = null
                        showNewForm = false
                    }
                )
            }
        }
        items(state.profiles) { profile ->
            ProfileCard(profile = profile, onEdit = {
                editing = profile
                showNewForm = false
            })
        }
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
private fun ProfileEditor(
    initial: PersonProfile?,
    onCancel: () -> Unit,
    onSave: (PersonProfile) -> Unit
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var age by remember(initial?.id) { mutableStateOf((initial?.age ?: 25).toString()) }
    var education by remember(initial?.id) { mutableStateOf(initial?.education.orEmpty()) }
    var job by remember(initial?.id) { mutableStateOf(initial?.job.orEmpty()) }
    var city by remember(initial?.id) { mutableStateOf(initial?.city.orEmpty()) }
    var lifeGoals by remember(initial?.id) { mutableStateOf(initial?.lifeGoals.orEmpty()) }
    var interests by remember(initial?.id) { mutableStateOf(initial?.interests.orEmpty()) }
    var values by remember(initial?.id) { mutableStateOf(initial?.values.orEmpty()) }
    var beliefs by remember(initial?.id) { mutableStateOf(initial?.beliefs.orEmpty()) }
    var communicationStyle by remember(initial?.id) { mutableStateOf(initial?.communicationStyle.orEmpty()) }
    var loveLanguage by remember(initial?.id) { mutableStateOf(initial?.loveLanguage.orEmpty()) }
    var personalityType by remember(initial?.id) { mutableStateOf(initial?.personalityType.orEmpty()) }
    var traits by remember(initial?.id) { mutableStateOf(initial?.traits.orEmpty()) }
    var dailyHabits by remember(initial?.id) { mutableStateOf(initial?.dailyHabits.orEmpty()) }
    var consent by remember(initial?.id) { mutableStateOf(initial?.consentGranted ?: false) }

    Card(modifier = Modifier.padding(16.dp)) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(if (initial == null) "پروفایل جدید" else "ویرایش پروفایل", fontWeight = FontWeight.SemiBold)
            ProfileField("نام", name) { name = it }
            OutlinedTextField(
                value = age,
                onValueChange = { age = it.filter(Char::isDigit).take(3) },
                label = { Text("سن") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            ProfileField("تحصیلات", education) { education = it }
            ProfileField("شغل", job) { job = it }
            ProfileField("محل زندگی", city) { city = it }
            ProfileField("اهداف زندگی", lifeGoals) { lifeGoals = it }
            ProfileField("علاقه‌ها", interests) { interests = it }
            ProfileField("ارزش‌های شخصی", values) { values = it }
            ProfileField("باورهای مهم", beliefs) { beliefs = it }
            ProfileField("سبک ارتباط", communicationStyle) { communicationStyle = it }
            ProfileField("زبان عشق", loveLanguage) { loveLanguage = it }
            ProfileField("تیپ شخصیتی", personalityType) { personalityType = it }
            ProfileField("ویژگی‌های اخلاقی", traits) { traits = it }
            ProfileField("عادات روزانه", dailyHabits) { dailyHabits = it }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Switch(checked = consent, onCheckedChange = { consent = it })
                Text("رضایت می‌دهم داده‌های من در تحلیل مشترک استفاده شود.")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onCancel) { Text("لغو") }
                Button(
                    enabled = name.isNotBlank(),
                    onClick = {
                        onSave(
                            PersonProfile(
                                id = initial?.id ?: UUID.randomUUID().toString(),
                                name = name,
                                age = age.toIntOrNull() ?: 0,
                                education = education,
                                job = job,
                                city = city,
                                lifeGoals = lifeGoals,
                                interests = interests,
                                values = values,
                                beliefs = beliefs,
                                communicationStyle = communicationStyle,
                                loveLanguage = loveLanguage,
                                personalityType = personalityType,
                                traits = traits,
                                dailyHabits = dailyHabits,
                                consentGranted = consent
                            )
                        )
                    }
                ) { Text("ذخیره") }
            }
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ProfileCard(profile: PersonProfile, onEdit: () -> Unit) {
    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(profile.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onEdit) { Text("ویرایش") }
            }
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
