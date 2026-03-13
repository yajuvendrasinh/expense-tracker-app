package com.expense.tracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.expense.tracker.data.repository.SmsRepository
import com.expense.tracker.util.TransactionParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.expense.tracker.util.SmsMessage

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var smsRepository: SmsRepository

    /**
     * Called when an SMS is received.
     * Filters messages based on keywords (e.g., "debited", "spent") and parses them to extract transaction details.
     */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val body = sms.messageBody
                val timestamp = sms.timestampMillis
                // Basic filtering: Check for currency symbols or keywords to identify potential transactions
                if (body.contains("Rs.", ignoreCase = true) || 
                    body.contains("INR", ignoreCase = true) || 
                    body.contains("debited", ignoreCase = true) ||
                    body.contains("spent", ignoreCase = true)) {
                    
                    val smsMessage = SmsMessage(body, timestamp)
                    val details = TransactionParser.parse(smsMessage)
                    if (details != null) {
                        // Emit to repository to be observed by UI/ViewModel
                        CoroutineScope(Dispatchers.IO).launch {
                            smsRepository.emitTransaction(details)
                        }
                    }
                }
            }
        }
    }
}
