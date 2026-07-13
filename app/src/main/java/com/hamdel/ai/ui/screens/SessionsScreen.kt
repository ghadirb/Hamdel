package com.hamdel.ai.ui.screens

import android.Manifest
import android.media.MediaRecorder
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamdel.ai.ui.RelationshipViewModel
import com.hamdel.ai.ui.components.ScreenFrame
import java.io.File

@Composable
fun SessionsScreen(viewModel: RelationshipViewModel, padding: PaddingValues) {
    val context = LocalContext.current
    val status by viewModel.statusMessage.collectAsState()
    val transcript by viewModel.transcribedText.collectAsState()
    val busy by viewModel.isBusy.collectAsState()
    val dashboard by viewModel.dashboard.collectAsState()
    val consentReady = dashboard.profiles.size >= 2 && dashboard.profiles.all { it.consentGranted }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var permissionPending by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionPending = false
        if (granted) {
            val file = File(context.cacheDir, "hamdel-session-${System.currentTimeMillis()}.m4a")
            runCatching {
                MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(file.absolutePath)
                    prepare()
                    start()
                }
            }.onSuccess { activeRecorder ->
                recorder = activeRecorder
                recordedFile = file
                isRecording = true
                viewModel.setStatus("ضبط صدا آغاز شد.")
            }.onFailure {
                recorder?.release()
                recorder = null
                viewModel.setStatus("شروع ضبط ممکن نشد. مجوز میکروفن را بررسی کنید.")
            }
        } else {
            viewModel.setStatus("برای ضبط جلسه، مجوز میکروفن لازم است.")
        }
    }

    val audioFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val extension = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(context.contentResolver.getType(it))
                ?.takeIf { value -> value.length <= 5 }
                ?: "m4a"
            val file = File(context.cacheDir, "hamdel-import-${System.currentTimeMillis()}.$extension")
            context.contentResolver.openInputStream(it)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            recordedFile = file
            viewModel.transcribeAndAnalyze("فایل صوتی جلسه", file)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder?.runCatching {
                stop()
                release()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(bottom = 18.dp)
    ) {
        item {
            ScreenFrame("جلسات گفتگو", "جلسه را ضبط کنید یا فایل صوتی بدهید؛ پس از پایان، متن و تحلیل جلسه در حافظه رابطه ذخیره می‌شود.") {
                if (!consentReady) {
                    Text("برای ضبط و تحلیل جلسه، ابتدا دو پروفایل بسازید و رضایت تحلیل هر دو نفر را فعال کنید.", color = MaterialTheme.colorScheme.error)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        enabled = !busy && !isRecording && consentReady && !permissionPending,
                        onClick = {
                            permissionPending = true
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    ) {
                        Icon(Icons.Outlined.Mic, contentDescription = null)
                        Text("شروع ضبط")
                    }
                    Button(
                        enabled = isRecording,
                        onClick = {
                            val didStop = recorder?.runCatching {
                                stop()
                                release()
                            }?.isSuccess == true
                            recorder = null
                            isRecording = false
                            if (didStop) {
                                recordedFile?.let { viewModel.transcribeAndAnalyze("جلسه ضبط‌شده", it) }
                            } else {
                                viewModel.setStatus("فایل صوتی معتبر ساخته نشد. ضبط را کمی طولانی‌تر انجام دهید و دوباره تلاش کنید.")
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.Stop, contentDescription = null)
                        Text("پایان و تحلیل")
                    }
                }
                if (isRecording) {
                    Text("ضبط صدا در حال انجام است.", color = MaterialTheme.colorScheme.primary)
                }
                OutlinedButton(enabled = !busy && !isRecording && consentReady, onClick = { audioFileLauncher.launch("audio/*") }) {
                    Icon(Icons.Outlined.AudioFile, contentDescription = null)
                    Text("انتخاب فایل صوتی")
                }
                status?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        item {
            Card(modifier = Modifier.padding(16.dp)) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("رونویسی آخرین جلسه", fontWeight = FontWeight.SemiBold)
                    Text(transcript ?: "هنوز جلسه‌ای رونویسی نشده است.")
                }
            }
        }
    }
}
