package com.hamdel.ai.data.sms

import android.content.Context
import android.provider.ContactsContract
import android.provider.Telephony
import com.hamdel.ai.data.model.ContactMessage
import com.hamdel.ai.data.settings.HamdelPreferences
import java.security.MessageDigest

class ContactSmsImporter(private val context: Context) {
    fun importForConfiguredContact(): List<ContactMessage> {
        val preferences = HamdelPreferences(context)
        if (!preferences.messageSyncEnabled || preferences.monitoredContactName.isBlank()) return emptyList()
        return importForExactName(preferences.monitoredContactName)
    }

    fun importForExactName(contactName: String): List<ContactMessage> {
        val numbers = phoneNumbersForExactName(contactName)
        if (numbers.isEmpty()) return emptyList()
        val results = mutableListOf<ContactMessage>()
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE),
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val typeIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            while (cursor.moveToNext()) {
                val address = cursor.getString(addressIndex).orEmpty()
                if (normalize(address) !in numbers) continue
                val body = cursor.getString(bodyIndex).orEmpty()
                val timestamp = cursor.getLong(dateIndex)
                val direction = if (cursor.getInt(typeIndex) == Telephony.Sms.MESSAGE_TYPE_SENT) "ارسالی" else "دریافتی"
                results += ContactMessage(
                    id = sha256("$address|$body|$timestamp"),
                    contactName = contactName,
                    address = address,
                    body = body,
                    timestamp = timestamp,
                    direction = direction
                )
            }
        }
        return results
    }

    private fun phoneNumbersForExactName(name: String): Set<String> {
        val values = mutableSetOf<String>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} = ?",
            arrayOf(name.trim()),
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) values += normalize(cursor.getString(index).orEmpty())
        }
        return values
    }

    private fun normalize(number: String): String = number.filter(Char::isDigit).takeLast(10)

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
