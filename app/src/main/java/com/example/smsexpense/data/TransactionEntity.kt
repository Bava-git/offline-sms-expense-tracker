package com.example.smsexpense.data

data class TransactionEntity(
    val id: Long = 0,
    val smsHash: String,
    val sender: String,
    val amount: Long,
    val currency: String = "INR",
    val type: String,
    val merchant: String? = null,
    val accountLast4: String? = null,
    val balanceAfter: Long? = null,
    val timestamp: Long,
    val categoryId: Long? = null,
    val status: String = "PENDING_CATEGORY",
    val note: String? = null,
    val counterparty: String? = null,
    val upiReference: String? = null,
    val rawBody: String
)