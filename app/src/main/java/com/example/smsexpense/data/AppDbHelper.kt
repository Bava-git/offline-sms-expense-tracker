package com.example.smsexpense.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

object AppDbHelper {

    private const val DB_NAME = "sms_expense.db"
    private const val DB_VERSION = 2

    // --- Schema ---

    private val CREATE_TRANSACTIONS = """
        CREATE TABLE IF NOT EXISTS transactions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            sms_hash TEXT NOT NULL UNIQUE,
            sender TEXT NOT NULL,
            amount INTEGER NOT NULL,
            currency TEXT NOT NULL DEFAULT 'INR',
            type TEXT NOT NULL,
            merchant TEXT,
            account_last4 TEXT,
            balance_after INTEGER,
            timestamp INTEGER NOT NULL,
            category_id INTEGER,
            status TEXT NOT NULL DEFAULT 'PENDING_CATEGORY',
            note TEXT,
            counterparty TEXT,
            upi_reference TEXT,
            raw_body TEXT NOT NULL
        )
    """.trimIndent()

    private val CREATE_CATEGORIES = """
        CREATE TABLE IF NOT EXISTS categories (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            kind TEXT NOT NULL,
            icon TEXT NOT NULL,
            is_default INTEGER NOT NULL DEFAULT 0
        )
    """.trimIndent()

    private val CREATE_MERCHANT_RULES = """
        CREATE TABLE IF NOT EXISTS merchant_rules (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            merchant TEXT NOT NULL UNIQUE,
            category_id INTEGER NOT NULL
        )
    """.trimIndent()

    private const val INDEX_TX_TIMESTAMP = "CREATE INDEX IF NOT EXISTS idx_tx_timestamp ON transactions(timestamp)"
    private const val INDEX_TX_STATUS = "CREATE INDEX IF NOT EXISTS idx_tx_status ON transactions(status)"
    private const val INDEX_TX_CATEGORY = "CREATE INDEX IF NOT EXISTS idx_tx_category ON transactions(category_id)"
    private const val INDEX_TX_SMSHASH = "CREATE UNIQUE INDEX IF NOT EXISTS idx_tx_smshash ON transactions(sms_hash)"

    // --- Column names ---

    object Tx {
        const val ID = "id"
        const val SMS_HASH = "sms_hash"
        const val SENDER = "sender"
        const val AMOUNT = "amount"
        const val CURRENCY = "currency"
        const val TYPE = "type"
        const val MERCHANT = "merchant"
        const val ACCOUNT_LAST4 = "account_last4"
        const val BALANCE_AFTER = "balance_after"
        const val TIMESTAMP = "timestamp"
        const val CATEGORY_ID = "category_id"
        const val STATUS = "status"
        const val NOTE = "note"
        const val RAW_BODY = "raw_body"
    }

    object Cat {
        const val ID = "id"
        const val NAME = "name"
        const val KIND = "kind"
        const val ICON = "icon"
        const val IS_DEFAULT = "is_default"
    }

    object Rule {
        const val ID = "id"
        const val MERCHANT = "merchant"
        const val CATEGORY_ID = "category_id"
    }

    internal class OpenHelper internal constructor(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(CREATE_TRANSACTIONS)
            db.execSQL(CREATE_CATEGORIES)
            db.execSQL(CREATE_MERCHANT_RULES)
            db.execSQL(INDEX_TX_TIMESTAMP)
            db.execSQL(INDEX_TX_STATUS)
            db.execSQL(INDEX_TX_CATEGORY)
            db.execSQL(INDEX_TX_SMSHASH)
            seedDefaultCategories(db)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion < 2) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN counterparty TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN upi_reference TEXT")
            }
        }

        private fun seedDefaultCategories(db: SQLiteDatabase) {
            val expenseIcons = listOf("🍔", "🛒", "🚌", "⛽", "🛍️", "💡", "🏠", "🏥", "🎬", "📚", "✈️", "🏦", "📱", "🏧", "📦")
            val incomeIcons = listOf("💼", "💼", "💰", "💳", "🎁", "📦")
            val expenseNames = listOf(
                "Food", "Groceries", "Transport", "Fuel", "Shopping", "Bills & Utilities", "Rent",
                "Health", "Entertainment", "Education", "Travel", "EMI/Loan", "Subscriptions",
                "Cash Withdrawal", "Other"
            )
            val incomeNames = listOf("Salary", "Business", "Refund", "Interest", "Gift", "Other")

            val values = android.content.ContentValues()
            for ((name, icon) in expenseNames.zip(expenseIcons)) {
                values.clear()
                values.put(Cat.NAME, name)
                values.put(Cat.KIND, "EXPENSE")
                values.put(Cat.ICON, icon)
                values.put(Cat.IS_DEFAULT, 1)
                db.insert("categories", null, values)
            }
            for ((name, icon) in incomeNames.zip(incomeIcons)) {
                values.clear()
                values.put(Cat.NAME, name)
                values.put(Cat.KIND, "INCOME")
                values.put(Cat.ICON, icon)
                values.put(Cat.IS_DEFAULT, 1)
                db.insert("categories", null, values)
            }
        }
    }

    @Volatile private var instance: OpenHelper? = null

    fun init(context: Context) {
        instance = OpenHelper(context.applicationContext)
    }

    fun db(): SQLiteDatabase = instance?.writableDatabase ?: error("AppDbHelper not initialized")
}