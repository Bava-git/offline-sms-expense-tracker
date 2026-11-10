package com.example.smsexpense.data

object TransactionRepositoryFactory {
    @JvmStatic fun get(context: android.content.Context): TransactionRepository {
        AppDbHelper.init(context)
        return TransactionRepository(context)
    }
}
