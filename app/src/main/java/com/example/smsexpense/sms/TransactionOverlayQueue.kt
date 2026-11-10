package com.example.smsexpense.sms

import android.content.Context

object TransactionOverlayQueue {
    private const val PREFERENCES = "transaction_overlay_queue"
    private const val KEY_PENDING_IDS = "pending_transaction_ids"
    private const val KEY_ACTIVE_ID = "active_transaction_id"
    private val lock = Any()

    fun enqueue(context: Context, transactionIds: List<Long>) {
        if (transactionIds.isEmpty()) return
        synchronized(lock) {
            val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            val pending = readPending(preferences.getString(KEY_PENDING_IDS, null))
            val activeId = preferences.getLong(KEY_ACTIVE_ID, NO_ACTIVE_ID)
            transactionIds
                .filter { it > 0L && it != activeId && it !in pending }
                .forEach(pending::add)
            check(
                preferences.edit()
                    .putString(KEY_PENDING_IDS, pending.joinToString(","))
                    .commit()
            ) { "Could not persist pending transaction overlays." }
        }
    }

    fun currentOrNext(context: Context): Long? = synchronized(lock) {
        val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val activeId = preferences.getLong(KEY_ACTIVE_ID, NO_ACTIVE_ID)
        if (activeId > 0L) return@synchronized activeId

        val pending = readPending(preferences.getString(KEY_PENDING_IDS, null))
        if (pending.isEmpty()) return@synchronized null
        val nextId = pending.removeAt(0)
        check(
            preferences.edit()
                .putLong(KEY_ACTIVE_ID, nextId)
                .putString(KEY_PENDING_IDS, pending.joinToString(","))
                .commit()
        ) { "Could not activate the next transaction overlay." }
        nextId
    }

    fun complete(context: Context, transactionId: Long) {
        synchronized(lock) {
            val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            if (preferences.getLong(KEY_ACTIVE_ID, NO_ACTIVE_ID) != transactionId) return
            check(
                preferences.edit()
                    .remove(KEY_ACTIVE_ID)
                    .commit()
            ) { "Could not complete the current transaction overlay." }
        }
    }

    private fun readPending(value: String?): MutableList<Long> =
        value.orEmpty()
            .split(',')
            .mapNotNull(String::toLongOrNull)
            .filter { it > 0L }
            .distinct()
            .toMutableList()

    private const val NO_ACTIVE_ID = -1L
}
