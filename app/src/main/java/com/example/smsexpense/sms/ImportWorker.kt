package com.example.smsexpense.sms

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class ImportWorker(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {
    override fun doWork(): Result {
        return Result.success()
    }
}
