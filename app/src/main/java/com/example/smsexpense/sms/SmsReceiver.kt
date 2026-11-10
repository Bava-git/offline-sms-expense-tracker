package com.example.smsexpense.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.provider.Telephony
import com.example.smsexpense.data.TransactionEntity
import com.example.smsexpense.data.TransactionRepositoryFactory
import com.example.smsexpense.parser.BankSmsParserImplementation
import com.example.smsexpense.util.AppLogger
import com.example.smsexpense.util.Hashing

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        AppLogger.log(context, "SmsTracker", "New SMS received")
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val parser = BankSmsParserImplementation()
        val insertedTransactionIds = mutableListOf<Long>()
        for (sms in messages) {
            val body = sms.messageBody ?: continue
            val sender = sms.originatingAddress ?: ""
            val ts = sms.timestampMillis
            val c = parser.classify(body, sender, ts)
            AppLogger.log(context, "SmsTracker", "SMS from $sender classified=${c.isBank}")
            if (c.isBank) {
                val transaction = TransactionEntity(
                    smsHash = Hashing.sha256("$sender|$body|$ts"),
                    sender = sender,
                    amount = parser.extractAmount(body) ?: 0L,
                    type = parser.extractDirection(body),
                    merchant = parser.extractMerchant(body),
                    accountLast4 = parser.extractAccountLast4(body),
                    balanceAfter = parser.extractBalanceAfter(body),
                    timestamp = ts,
                    counterparty = parser.extractCounterparty(body),
                    upiReference = parser.extractUpiReference(body),
                    rawBody = body
                )
                val inserted = TransactionRepositoryFactory.get(context).insert(transaction)
                AppLogger.log(context, "SmsTracker", "Transaction stored=$inserted")
                if (inserted > 0) insertedTransactionIds += inserted
            }
        }
        if (insertedTransactionIds.isNotEmpty()) {
            if (Settings.canDrawOverlays(context)) {
                try {
                    TransactionOverlayQueue.enqueue(context, insertedTransactionIds)
                    context.startActivity(
                        Intent(context, SmsTransactionOverlayActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    )
                } catch (error: RuntimeException) {
                    AppLogger.log(context, "SmsTracker", "Could not queue or show transaction overlay: ${error.message}")
                }
            } else {
                AppLogger.log(context, "SmsTracker", "Transaction overlay skipped: display-over-other-apps access not granted")
            }
        }
    }
}
