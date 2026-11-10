package com.example.smsexpense.sms

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.example.smsexpense.MainActivity
import com.example.smsexpense.data.TransactionRepositoryFactory
import com.example.smsexpense.util.AppLogger
import java.util.concurrent.TimeUnit

class TransactionReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    override fun doWork(): Result {
        val transactionId = inputData.getLong(KEY_TRANSACTION_ID, -1L)
        if (transactionId < 0L) return Result.failure()

        val transaction = TransactionRepositoryFactory.get(applicationContext)
            .getAll()
            .firstOrNull { it.id == transactionId }
            ?: return Result.success()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            AppLogger.log(applicationContext, "SmsTracker", "Reminder skipped: notification permission not granted")
            return Result.success()
        }

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Transaction reminders", NotificationManager.IMPORTANCE_HIGH)
        )
        val openApp = PendingIntent.getActivity(
            applicationContext,
            transactionId.toInt(),
            Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val label = transaction.counterparty?.takeIf(String::isNotBlank)
            ?: transaction.merchant?.takeIf(String::isNotBlank)
            ?: "New transaction"
        NotificationManagerCompat.from(applicationContext).notify(
            transactionId.toInt(),
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Transaction reminder")
                .setContentText("Review $label in SMS Expense Tracker")
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        )
        return Result.success()
    }

    companion object {
        private const val CHANNEL_ID = "transaction_reminders"
        private const val KEY_TRANSACTION_ID = "transaction_id"

        fun enqueue(context: Context, transactionId: Long) {
            val request = OneTimeWorkRequestBuilder<TransactionReminderWorker>()
                .setInitialDelay(15, TimeUnit.MINUTES)
                .setInputData(Data.Builder().putLong(KEY_TRANSACTION_ID, transactionId).build())
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
