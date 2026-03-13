package com.expense.tracker.util

import java.util.regex.Pattern

data class TransactionDetails(
    val amount: Double,
    val vendor: String,
    val upiId: String? = null,
    val paymentMethod: String? = null,
    val expenseTimestamp: String? = null,
    val timestamp: Long,
    val originalMessage: String
)

/**
 * Utility object for parsing transaction details from SMS strings.
 * It uses regular expressions to extract the amount and vendor information.
 */
object TransactionParser {
    // Patterns for Indian banks (HDFC, SBI, ICICI, etc.)
    // Examples:
    // "Rs. 123.00 spent on card XX at Amazon..."
    // "Debited Rs 500.00 VPA payee@okaxis..."
    // "Sent Rs. 1000.00 to FRIEND..."
    // "Acct XXdebited for Rs. 199.00 on 12-02-23..."

    private val PATTERNS = listOf(
        // Specific format: "Txn Rs.130.00 On HDFC Bank Card 9131 At gpay-12197656097@okbizaxi by UPI 607188325012 On 12-03"
        Pattern.compile("(?i)Txn\\s+Rs\\.?(\\d+(?:,\\d+)*(?:\\.\\d+)?)\\s+On\\s+(.*?)\\s+At\\s+(.*?)\\s+by\\s+UPI\\s+(\\d+)\\s+On\\s+(\\d{2}-\\d{2})"),
        // Specific format: "Spent Rs.96.65 On HDFC Bank Card 9131 At ZOMATO On 2026-02-24:19:43:06.Not You?..."
        Pattern.compile("(?i)spent\\s+(?:Rs\\.?|INR)\\s*(\\d+(?:,\\d+)*(?:\\.\\d+)?)\\s+on\\s+(.*?)\\s+at\\s+(.*?)\\s+on\\s+(\\d{4}-\\d{2}-\\d{2}:\\d{2}:\\d{2}:\\d{2})"), // 4 Groups
        // "spent Rs. 500 at Amazon"
        Pattern.compile("(?i)spent\\s+(?:Rs\\.?|INR)\\s*(\\d+(?:,\\d+)*(?:\\.\\d+)?)\\s+(?:on|at|to)\\s+([^.]+)"),
        // "Debited Rs 500 VPA amazon@"
        Pattern.compile("(?i)debited\\s+(?:by|for)?\\s*(?:Rs\\.?|INR)\\s*(\\d+(?:,\\d+)*(?:\\.\\d+)?)\\s+(?:VPA|to|at)\\s+([^.]+)"),
        // "Rs. 500 debited from..." (Harder to get vendor, usually at end)
        Pattern.compile("(?i)(?:Rs\\.?|INR)\\s*(\\d+(?:,\\d+)*(?:\\.\\d+)?)\\s+debited.*?(?:to|at)\\s+([^.]+)"),
        // UPI: "Paid Rs. 500 to MERCHANT"
        Pattern.compile("(?i)Paid\\s+(?:Rs\\.?|INR)\\s*(\\d+(?:,\\d+)*(?:\\.\\d+)?)\\s+(?:to|at)\\s+([^.]+)"),
        // Generic: "Sent Rs. 500" or "Transfer Rs. 500"
        Pattern.compile("(?i)(?:Sent|Transfer|Paid|Debited).*?(?:Rs\\.?|INR)\\s*(\\d+(?:,\\d+)*(?:\\.\\d+)?).*?to\\s+([^.]+)"),
        // Catch-all: Any number with decimal and 2 digits (e.g., 123.45) if other patterns fail
        // This is a broad heuristic requested by user: "at least one digit followed by decimal and two digit"
        Pattern.compile("(\\d+(?:,\\d+)*\\.\\d{2})")
    )

    /**
     * Parses an [SmsMessage] to extract transaction details.
     *
     * @param sms The SMS message object containing the body and timestamp.
     * @return A [TransactionDetails] object if a valid transaction is found, or null otherwise.
     */
    fun parse(sms: SmsMessage): TransactionDetails? {
        val message = sms.body
        
        // Exclude credit transactions
        if (message.contains("credited", ignoreCase = true)) {
            android.util.Log.d("TransactionParser", "Skipping 'credited' message: ${message.take(20)}...")
            return null
        }

        // Exclude OTP messages
        if (message.contains(Regex("\\botp\\b", RegexOption.IGNORE_CASE))) {
            android.util.Log.d("TransactionParser", "Skipping 'OTP' message: ${message.take(20)}...")
            return null
        }

        // Normalize
        val cleanMessage = message.replace(",", "") // Remove commas from numbers

        for (pattern in PATTERNS) {
            val matcher = pattern.matcher(cleanMessage)
            if (matcher.find()) {
                try {
                    val amountStr = matcher.group(1)
                    
                    var vendorStr: String
                    var paymentMethodStr: String? = null
                    var expenseTimestampStr: String? = null
                    var upiIdStr: String? = null

                    if (pattern.pattern().contains("Txn\\\\s+Rs")) {
                        // New pattern match for specific UPI SMS format
                        // Groups: 1=Amount, 2=PaymentMethod, 3=Vendor/At, 4=UPI Txn ID, 5=Date
                        paymentMethodStr = matcher.group(2)?.trim()
                        vendorStr = matcher.group(3)?.trim() ?: "Unknown"
                        expenseTimestampStr = (matcher.group(5) ?: "") + " " + (matcher.group(4) ?: "")
                    } else if (matcher.groupCount() >= 4) {
                        // Pattern match for specific SMS format
                        paymentMethodStr = matcher.group(2)?.trim()
                        vendorStr = matcher.group(3)?.trim() ?: "Unknown"
                        expenseTimestampStr = matcher.group(4)?.trim()
                    } else if (matcher.groupCount() >= 2) {
                        vendorStr = matcher.group(2)?.trim() ?: "Unknown"
                    } else {
                        vendorStr = "Unknown" // For catch-all pattern
                    }
                    
                    // Prefix with UPI if "UPI" is in the message
                    if (message.contains("UPI", ignoreCase = true) && paymentMethodStr != null) {
                        if (!paymentMethodStr.startsWith("UPI", ignoreCase = true)) {
                            paymentMethodStr = "UPI $paymentMethodStr"
                        }
                    }

                    // Extract UPI ID from vendor string if possible
                    val upiRegex = Regex("[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z0-9]{2,64}")
                    val upiMatch = upiRegex.find(vendorStr)
                    if (upiMatch != null) {
                        upiIdStr = upiMatch.value
                    }

                    val amount = amountStr?.toDoubleOrNull()
                    if (amount != null) {
                        android.util.Log.d("TransactionParser", "Matched: Amount=$amount, Vendor=$vendorStr, UPI ID=$upiIdStr, PaymentMethod=$paymentMethodStr")
                        return TransactionDetails(
                            amount = amount, 
                            vendor = vendorStr,
                            upiId = upiIdStr,
                            paymentMethod = paymentMethodStr,
                            expenseTimestamp = expenseTimestampStr,
                            timestamp = sms.timestamp,
                            originalMessage = message
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.w("TransactionParser", "Regex match failed", e)
                    continue
                }
            }
        }
        android.util.Log.d("TransactionParser", "No match found for: ${message.take(30)}...")
        return null
    }
}
