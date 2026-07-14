package com.hamdel.ai.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.hamdel.ai.data.local.HamdelDatabase
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManager {
    private const val TAG = "HamdelBackup"
    private const val DB_NAME = "hamdel.db"
    private const val SETTINGS_ENTRY = "hamdel_preferences.json"
    private val parts = listOf(DB_NAME, "$DB_NAME-wal", "$DB_NAME-shm")

    fun backupToUri(context: Context, uri: Uri): Boolean = runCatching {
        HamdelDatabase.getDatabase(context).openHelper.writableDatabase
            .query("PRAGMA wal_checkpoint(FULL)").close()
        val dbDir = context.getDatabasePath(DB_NAME).parentFile ?: return false
        context.contentResolver.openOutputStream(uri)?.use { output ->
            ZipOutputStream(output).use { zip ->
                parts.forEach { name ->
                    val file = File(dbDir, name)
                    if (file.exists()) {
                        zip.putNextEntry(ZipEntry(name))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
                zip.putNextEntry(ZipEntry(SETTINGS_ENTRY))
                zip.write(exportPreferences(context).toByteArray())
                zip.closeEntry()
            }
        } ?: return false
        true
    }.onFailure { Log.e(TAG, "Backup failed", it) }.getOrDefault(false)

    fun restoreFromUri(context: Context, uri: Uri): Boolean = runCatching {
        val dbDir = context.getDatabasePath(DB_NAME).parentFile ?: return false
        dbDir.mkdirs()
        HamdelDatabase.closeDatabase()
        var restored = false
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name in parts) {
                        File(dbDir, entry.name).outputStream().use { zip.copyTo(it) }
                        restored = true
                    } else if (entry.name == SETTINGS_ENTRY) {
                        importPreferences(context, zip.readBytes().toString(Charsets.UTF_8))
                    }
                    entry = zip.nextEntry
                }
            }
        } ?: return false
        restored
    }.onFailure { Log.e(TAG, "Restore failed", it) }.getOrDefault(false)

    private fun exportPreferences(context: Context): String {
        val values = context.getSharedPreferences("hamdel_preferences", Context.MODE_PRIVATE).all
        return JSONObject().apply {
            values.forEach { (key, value) -> put(key, value) }
        }.toString()
    }

    private fun importPreferences(context: Context, raw: String) {
        val json = JSONObject(raw)
        val editor = context.getSharedPreferences("hamdel_preferences", Context.MODE_PRIVATE).edit().clear()
        json.keys().forEach { key ->
            when (val value = json.get(key)) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                else -> editor.putString(key, value.toString())
            }
        }
        editor.apply()
    }
}
