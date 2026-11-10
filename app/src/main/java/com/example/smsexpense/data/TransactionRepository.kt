package com.example.smsexpense.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.smsexpense.parser.BankSmsParserImplementation

class TransactionRepository(private val context: Context) {
    private val db: SQLiteDatabase get() = AppDbHelper.db()
    init { AppDbHelper.init(context) }

    fun insert(txn: TransactionEntity): Long {
        val cv = ContentValues().apply {
            put("sms_hash", txn.smsHash)
            put("sender", txn.sender)
            put("amount", txn.amount)
            put("currency", txn.currency)
            put("type", txn.type)
            put("merchant", txn.merchant)
            put("account_last4", txn.accountLast4)
            put("balance_after", txn.balanceAfter)
            put("timestamp", txn.timestamp)
            put("category_id", txn.categoryId)
            put("status", txn.status)
            put("note", txn.note)
            put("counterparty", txn.counterparty)
            put("upi_reference", txn.upiReference)
            put("raw_body", txn.rawBody)
        }
        return db.insertWithOnConflict("transactions", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun getAll(): List<TransactionEntity> {
        val list = mutableListOf<TransactionEntity>()
        val c = db.rawQuery("SELECT * FROM transactions ORDER BY timestamp DESC", null)
        val idxId = c.getColumnIndex("id")
        val idxHash = c.getColumnIndex("sms_hash")
        val idxSender = c.getColumnIndex("sender")
        val idxAmount = c.getColumnIndex("amount")
        val idxType = c.getColumnIndex("type")
        val idxMerchant = c.getColumnIndex("merchant")
        val idxAcc = c.getColumnIndex("account_last4")
        val idxBal = c.getColumnIndex("balance_after")
        val idxTs = c.getColumnIndex("timestamp")
        val idxCat = c.getColumnIndex("category_id")
        val idxStatus = c.getColumnIndex("status")
        val idxNote = c.getColumnIndex("note")
        val idxCounterparty = c.getColumnIndex("counterparty")
        val idxReference = c.getColumnIndex("upi_reference")
        val idxRaw = c.getColumnIndex("raw_body")
        val parser = BankSmsParserImplementation()
        while (c.moveToNext()) {
            val rawBody = if (idxRaw >= 0) c.getString(idxRaw) else ""
            val storedAmount = if (idxAmount >= 0) c.getLong(idxAmount) else 0L
            list.add(TransactionEntity(
                id = if (idxId>=0) c.getLong(idxId) else 0,
                smsHash = if (idxHash>=0) c.getString(idxHash) else "",
                sender = if (idxSender>=0) c.getString(idxSender) else "",
                amount = if (storedAmount == 0L) parser.extractAmount(rawBody) ?: storedAmount else storedAmount,
                type = if (idxType>=0) c.getString(idxType) else "",
                merchant = if (idxMerchant>=0) c.getString(idxMerchant) else null,
                accountLast4 = (if (idxAcc>=0) c.getString(idxAcc) else null)
                    ?.takeIf(String::isNotBlank)
                    ?: parser.extractAccountLast4(rawBody),
                balanceAfter = if (idxBal>=0 && !c.isNull(idxBal)) c.getLong(idxBal) else null,
                timestamp = if (idxTs>=0) c.getLong(idxTs) else 0,
                categoryId = if (idxCat>=0 && !c.isNull(idxCat)) c.getLong(idxCat) else null,
                status = if (idxStatus>=0) c.getString(idxStatus) else "PENDING_CATEGORY",
                note = if (idxNote>=0) c.getString(idxNote) else null,
                counterparty = parser.extractCounterparty(rawBody)
                    ?: (if (idxCounterparty >= 0) c.getString(idxCounterparty) else null)
                        ?.takeIf(String::isNotBlank),
                upiReference = (if (idxReference>=0) c.getString(idxReference) else null)
                    ?: parser.extractUpiReference(rawBody),
                rawBody = rawBody
            ))
        }
        c.close()
        return list
    }

    fun getCategories(): List<CategoryEntity> {
        val categories = mutableListOf<CategoryEntity>()
        db.query(
            "categories",
            arrayOf("id", "name", "kind", "icon", "is_default"),
            null,
            null,
            null,
            null,
            "kind, name COLLATE NOCASE"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                categories += CategoryEntity(
                    id = cursor.getLong(0),
                    name = cursor.getString(1),
                    kind = cursor.getString(2),
                    icon = cursor.getString(3),
                    isDefault = cursor.getInt(4) != 0
                )
            }
        }
        return categories
    }

    fun addCategory(name: String, kind: String = "EXPENSE"): Long {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "Category name cannot be empty." }
        require(kind == "EXPENSE" || kind == "INCOME") { "Unsupported category type." }
        require(!categoryExists(normalizedName)) { "That category already exists." }
        val values = ContentValues().apply {
            put("name", normalizedName)
            put("kind", kind)
            put("icon", if (kind == "INCOME") "💰" else "📦")
            put("is_default", 0)
        }
        return db.insertOrThrow("categories", null, values)
    }

    fun renameCategory(id: Long, name: String) {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "Category name cannot be empty." }
        require(!categoryExists(normalizedName, exceptId = id)) { "That category already exists." }
        val values = ContentValues().apply { put("name", normalizedName) }
        check(db.update("categories", values, "id = ?", arrayOf(id.toString())) == 1) {
            "Category no longer exists."
        }
    }

    fun deleteCategory(id: Long): Boolean {
        val assigned = db.query(
            "transactions",
            arrayOf("id"),
            "category_id = ?",
            arrayOf(id.toString()),
            null,
            null,
            null,
            "1"
        ).use { it.moveToFirst() }
        if (assigned) return false
        return db.delete("categories", "id = ? AND is_default = 0", arrayOf(id.toString())) == 1
    }

    fun assignCategory(transactionId: Long, categoryId: Long?) {
        if (categoryId != null) {
            check(
                db.query(
                    "categories",
                    arrayOf("id"),
                    "id = ?",
                    arrayOf(categoryId.toString()),
                    null,
                    null,
                    null,
                    "1"
                ).use { it.moveToFirst() }
            ) { "Category no longer exists." }
        }
        val values = ContentValues().apply {
            if (categoryId == null) {
                putNull("category_id")
                put("status", "PENDING_CATEGORY")
            } else {
                put("category_id", categoryId)
                put("status", "CATEGORIZED")
            }
        }

        check(db.update("transactions", values, "id = ?", arrayOf(transactionId.toString())) == 1) {
            "Transaction no longer exists."
        }
    }

    fun updateNote(transactionId: Long, note: String?) {
        val normalized = note?.trim()?.takeIf { it.isNotEmpty() }
        val values = ContentValues().apply { put("note", normalized) }
        check(db.update("transactions", values, "id = ?", arrayOf(transactionId.toString())) == 1) {
            "Transaction no longer exists."
        }
    }

    private fun categoryExists(name: String, exceptId: Long? = null): Boolean {
        val selection = if (exceptId == null) {
            "name = ? COLLATE NOCASE"
        } else {
            "name = ? COLLATE NOCASE AND id != ?"
        }
        val args = if (exceptId == null) arrayOf(name) else arrayOf(name, exceptId.toString())
        return db.query("categories", arrayOf("id"), selection, args, null, null, null, "1")
            .use { it.moveToFirst() }
    }

    object Factory {
        @JvmStatic fun get(context: Context): TransactionRepository = TransactionRepository(context)
    }
}
