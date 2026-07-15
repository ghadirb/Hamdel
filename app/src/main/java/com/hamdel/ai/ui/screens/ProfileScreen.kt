package com.hamdel.ai.ui.screens

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hamdel.ai.data.model.PersonProfile
import com.hamdel.ai.data.billing.SubscriptionPlan
import com.hamdel.ai.data.billing.SubscriptionState
import com.hamdel.ai.ui.RelationshipViewModel
import com.hamdel.ai.ui.components.ScreenFrame
import java.util.UUID

@Composable
fun ProfileScreen(viewModel: RelationshipViewModel, padding: PaddingValues) {
    val context = LocalContext.current
    val state by viewModel.dashboard.collectAsState()
    val status by viewModel.statusMessage.collectAsState()
    val autoBackup by viewModel.autoBackupEnabled.collectAsState()
    val autoAnalysis by viewModel.autoAnalysisEnabled.collectAsState()
    val messageSync by viewModel.messageSyncEnabled.collectAsState()
    val savedContactName by viewModel.monitoredContactName.collectAsState()
    val busy by viewModel.isBusy.collectAsState()
    val subscription by viewModel.subscription.collectAsState()
    var editing by remember { mutableStateOf<PersonProfile?>(null) }
    var showNewForm by remember { mutableStateOf(false) }
    var contactName by remember(savedContactName) { mutableStateOf(savedContactName) }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            viewModel.backupTo(it)
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.restoreFrom(it) }
    }
    val smsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = result[Manifest.permission.READ_SMS] == true && result[Manifest.permission.READ_CONTACTS] == true
        viewModel.configureContactMessageSync(contactName, granted)
        if (granted) viewModel.importContactMessages()
        else viewModel.setStatus("برای هم‌رسانی پیام‌ها، مجوز پیامک و مخاطبان لازم است.")
    }

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
        item {
            SubscriptionCard(
                subscription = subscription,
                onSelectMonthly = { viewModel.purchaseSubscription(SubscriptionPlan.Monthly) },
                onSelectYearly = { viewModel.purchaseSubscription(SubscriptionPlan.Yearly) },
                onRestore = viewModel::restorePurchases
            )
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
        item {
            Card(modifier = Modifier.padding(16.dp)) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("پشتیبان‌گیری و هم‌رسانی", fontWeight = FontWeight.SemiBold)
                    Text("برای انتخاب Google Drive یا حافظه گوشی، مقصد را از انتخاب‌گر سیستم تعیین کنید.", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { backupLauncher.launch("hamdel-backup.zip") }) { Text("تهیه پشتیبان") }
                        OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }) { Text("بازیابی") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Switch(checked = autoBackup, onCheckedChange = viewModel::setAutoBackup)
                        Text("پشتیبان‌گیری خودکار روزانه")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Switch(checked = autoAnalysis, onCheckedChange = viewModel::setAutoAnalysis)
                        Text("تحلیل روزانه خودکار با رضایت دوطرفه")
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.padding(16.dp)) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("هم‌رسانی پیام‌های مخاطب", fontWeight = FontWeight.SemiBold)
                    Text("فقط با رضایت صریح شما و رضایت فعال هر دو پروفایل، پیام‌های SMS مخاطبی که نام کاملش را وارد می‌کنید وارد حافظه رابطه می‌شود.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text("نام دقیق مخاطب") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Switch(
                            checked = messageSync,
                            onCheckedChange = { enabled ->
                                if (enabled) smsPermissionLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS))
                                else viewModel.configureContactMessageSync(contactName, false)
                            }
                        )
                        Text("هم‌رسانی خودکار پیام‌ها")
                    }
                    OutlinedButton(
                        enabled = messageSync && contactName.isNotBlank(),
                        onClick = { viewModel.configureContactMessageSync(contactName, true); viewModel.importContactMessages() }
                    ) { Text("خواندن و هم‌رسانی اکنون") }
                }
            }
        }
        item {
            Card(modifier = Modifier.padding(16.dp)) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("پیشنهادهای پروفایل", fontWeight = FontWeight.SemiBold)
                    Text("مدل فقط پیشنهاد می‌دهد و هیچ فیلدی را بدون تأیید شما تغییر نمی‌دهد.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(
                        enabled = !busy && (state.contactMessages.isNotEmpty() || state.reports.isNotEmpty()),
                        onClick = viewModel::generateProfileSuggestions
                    ) { Text(if (busy) "در حال بررسی..." else "ساخت پیشنهاد از پیام‌ها و گفتگوها") }
                    if (state.profileSuggestions.isEmpty()) {
                        Text("هنوز پیشنهادی ثبت نشده است.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        state.profileSuggestions.forEach { suggestion ->
                            Text("${suggestion.profileName} - ${profileFieldLabel(suggestion.field)}", fontWeight = FontWeight.Medium)
                            Text(suggestion.proposedValue)
                            Text("دلیل: ${suggestion.reason}", style = MaterialTheme.typography.bodySmall)
                            Text("اطمینان: ${(suggestion.confidence * 100).toInt()}٪", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { viewModel.applyProfileSuggestion(suggestion.id) }) { Text("تأیید و اعمال") }
                                OutlinedButton(onClick = { viewModel.dismissProfileSuggestion(suggestion.id) }) { Text("رد") }
                            }
                        }
                    }
                }
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
                    Text("پیام‌های ذخیره‌شده: ${state.contactMessages.size}", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun SubscriptionCard(
    subscription: SubscriptionState,
    onSelectMonthly: () -> Unit,
    onSelectYearly: () -> Unit,
    onRestore: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("اشتراک و سهمیه هوش مصنوعی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            val planText = when (subscription.plan) {
                SubscriptionPlan.Monthly -> "دسترسی ویژه ماهانه فعال است"
                SubscriptionPlan.Yearly -> "دسترسی ویژه سالانه فعال است"
                SubscriptionPlan.Free -> "${subscription.freeCreditsRemaining} استفاده رایگان باقی مانده"
            }
            Text(planText, style = MaterialTheme.typography.bodyMedium)
            subscription.expiresAtMillis?.let { expiresAt ->
                Text("تا تاریخ ${formatExpiry(expiresAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(subscription.status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PlanOption(
                    modifier = Modifier.weight(1f),
                    title = "ماهانه",
                    isActive = subscription.plan == SubscriptionPlan.Monthly,
                    onClick = onSelectMonthly
                )
                PlanOption(
                    modifier = Modifier.weight(1f),
                    title = "سالانه",
                    badge = "بیشترین صرفه‌جویی",
                    isActive = subscription.plan == SubscriptionPlan.Yearly,
                    onClick = onSelectYearly
                )
            }

            TextButton(
                onClick = onRestore,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("بازیابی خریدهای قبلی")
            }

            Text(
                "قیمت و شرایط نهایی فقط در صفحه پرداخت فروشگاه (بازار یا مایکت) نمایش داده می‌شود.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatExpiry(epochMillis: Long): String {
    val formatter = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.US)
    return formatter.format(java.util.Date(epochMillis))
}

@Composable
private fun PlanOption(
    modifier: Modifier = Modifier,
    title: String,
    badge: String? = null,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Card(
        onClick = onClick,
        modifier = modifier,
        border = BorderStroke(if (isActive) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        )
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                badge ?: " ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(
                if (isActive) "فعال" else " ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
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

private fun profileFieldLabel(field: String): String = when (field) {
    "communicationStyle" -> "سبک ارتباط"
    "loveLanguage" -> "زبان عشق"
    "traits" -> "ویژگی‌های اخلاقی"
    "dailyHabits" -> "عادت‌های روزانه"
    "personalityType" -> "تیپ شخصیتی"
    else -> field
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
