package com.hamdel.ai.data.settings

import android.content.Context

class HamdelPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("hamdel_preferences", Context.MODE_PRIVATE)

    var backupUri: String?
        get() = prefs.getString(KEY_BACKUP_URI, null)
        set(value) = prefs.edit().putString(KEY_BACKUP_URI, value).apply()

    var autoBackupEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_BACKUP, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_BACKUP, value).apply()

    var messageSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_MESSAGE_SYNC, false)
        set(value) = prefs.edit().putBoolean(KEY_MESSAGE_SYNC, value).apply()

    var monitoredContactName: String
        get() = prefs.getString(KEY_CONTACT_NAME, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_CONTACT_NAME, value.trim()).apply()

    companion object {
        private const val KEY_BACKUP_URI = "backup_uri"
        private const val KEY_AUTO_BACKUP = "auto_backup"
        private const val KEY_MESSAGE_SYNC = "message_sync"
        private const val KEY_CONTACT_NAME = "monitored_contact_name"
    }
}
