package com.example.smsexpense.sms

import android.content.Context
import android.provider.Telephony
import com.example.smsexpense.data.TransactionEntity
import com.example.smsexpense.data.TransactionRepository
import com.example.smsexpense.parser.BankSmsParserImplementation
import com.example.smsexpense.util.AppLogger
import com.example.smsexpense.util.Hashing

class SmsInboxReader(private val repository: TransactionRepository) {
    fun importOldSms(context: Context, batchSize: Int = 200) {
        AppLogger.log(context, "SmsTracker", "Start reading existing SMS")
        val parser = BankSmsParserImplementation()
        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
            null, null, "${Telephony.Sms.DATE} DESC"
        )
        var count = 0
        var inserted = 0
        cursor?.use {
            while (it.moveToNext()) {
                val sender = it.getString(0) ?: ""
                val body = it.getString(1) ?: ""
                val ts = it.getLong(2)
                val c = parser.classify(body, sender, ts)
                if (c.isBank) {
                    val hash = Hashing.sha256("$sender|$body|$ts")
                    val txn = TransactionEntity(
                        smsHash = hash,
                        sender = sender,
                        amount = parser.extractAmount(body) ?: 0,
                        type = parser.extractDirection(body),
                        merchant = parser.extractMerchant(body),
                        accountLast4 = parser.extractAccountLast4(body),
                        counterparty = parser.extractCounterparty(body),
                        upiReference = parser.extractUpiReference(body),
                        balanceAfter = parser.extractBalanceAfter(body),
                        timestamp = ts,
                        rawBody = body
                    )
                    val r = repository.insert(txn)
                    if (r > 0) inserted++
                }
                count++
                if (count >= batchSize) break
            }
        }
        AppLogger.log(context, "SmsTracker", "Read $count SMS, inserted $inserted")
    }
}
