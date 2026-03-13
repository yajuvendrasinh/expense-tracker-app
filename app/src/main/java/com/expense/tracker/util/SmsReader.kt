package com.expense.tracker.util

import android.content.Context
import android.net.Uri
import android.provider.Telephony

data class SmsMessage(
    val body: String,
    val timestamp: Long
)

/**
 * Utility object for reading SMS messages from the device's inbox.
 * This handles the interaction with the Android ContentProvider.
 */
object SmsReader {

    /**
     * Reads SMS messages from the inbox received in the last 24 hours.
     *
     * @param context The application context required to access the ContentResolver.
     * @return A list of [SmsMessage] objects containing the body and timestamp of each message.
     */
    fun readRecentMessages(context: Context): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("body", "date")
        
        // Filter for messages in the last 24 hours as requested by user
        val splitTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        val selection = "date > ?"
        val selectionArgs = arrayOf(splitTime.toString())
        val sortOrder = "date DESC"

        try {
            android.util.Log.d("SmsReader", "Querying SMS since: $splitTime")
            val cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use {
                val bodyIndex = it.getColumnIndex("body")
                val dateIndex = it.getColumnIndex("date")
                android.util.Log.d("SmsReader", "Found ${it.count} messages in inbox.")
                
                while (it.moveToNext()) {
                    if (bodyIndex != -1 && dateIndex != -1) {
                        val body = it.getString(bodyIndex)
                        val date = it.getLong(dateIndex)
                        messages.add(SmsMessage(body, date))
                        // Log first 50 chars of message for debugging
                        val snippet = if (body.length > 50) body.take(50) + "..." else body
                        android.util.Log.d("SmsReader", "Read: $snippet")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SmsReader", "Error reading SMS", e)
            e.printStackTrace()
        }
        return messages
    }
}
