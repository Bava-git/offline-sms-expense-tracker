package com.example.smsexpense.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.smsexpense.data.TransactionEntity

object CategoryPromptNotifier {

    const val CHANNEL_ID = "new_transaction"
    const val CHANNEL_NAME = "New transaction"
    const val NOTIFICATION_ID = 1001

    private fun createChannelIfNeeded(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notify about new banking SMS transactions" }
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun show(ctx: Context, amount: Long, sender: String, direction: String, timestamp: Long) {
        createChannelIfNeeded(ctx)
        val manager = ctx.getSystemService(NotificationManager::class.java)
        val date = java.text.SimpleDateFormat("dd-MM-yyyy").format(java.util.Date(timestamp))
        val amountStr = if (direction == "DEBIT") "-$amount" else "+$amount"
        val title = when (direction) {
            "DEBIT" -> "Debited $amount.00"
            "CREDIT" -> "Credited $amount.00"
            else -> "New transaction"
        }
        val text = "$sender — $amountStr — $date"
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_add)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }
}