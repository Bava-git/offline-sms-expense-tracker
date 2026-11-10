package com.example.smsexpense.sms

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.smsexpense.data.TransactionEntity
import com.example.smsexpense.data.TransactionRepositoryFactory
import com.example.smsexpense.data.CategoryEntity
import com.example.smsexpense.ui.CategorizedSmsTransaction
import com.example.smsexpense.ui.Direction
import com.example.smsexpense.ui.SmsTransactionDraft
import com.example.smsexpense.ui.SmsTransactionOverlay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmsTransactionOverlayActivity : ComponentActivity() {
    private lateinit var repository: com.example.smsexpense.data.TransactionRepository
    private var currentOverlay by mutableStateOf<QueuedOverlay?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = TransactionRepositoryFactory.get(this)
        currentOverlay = loadNextOverlay()
        if (currentOverlay == null) {
            finish()
            return
        }

        setContent {
            MaterialTheme {
                currentOverlay?.let { overlay ->
                    val transaction = overlay.transaction
                    val categories = overlay.categories
                    val categoryKind = if (transaction.type.equals("DEBIT", true)) "EXPENSE" else "INCOME"
                    val availableCategories = categories.filter { it.kind == categoryKind }
                    val suggestedCategory = transaction.categoryId
                        ?.let { categoryId -> categories.firstOrNull { it.id == categoryId }?.name }
                        ?.takeIf { name -> availableCategories.any { it.name == name } }
                        .orEmpty()

                    SmsTransactionOverlay(
                        transaction = transaction.toOverlayDraft(),
                        categories = availableCategories.map { it.name },
                        suggestedCategory = suggestedCategory,
                        merchantAutoMapAvailable = false,
                        onSaveLedger = { categorized -> saveCategory(transaction, categorized) },
                        onRemindLater = {
                            TransactionReminderWorker.enqueue(this, transaction.id)
                            completeAndShowNext(transaction.id)
                        },
                        onMinimize = {
                            TransactionReminderWorker.enqueue(this, transaction.id)
                            completeAndShowNext(transaction.id)
                        },
                        onDismiss = { completeAndShowNext(transaction.id) }
                    )
                }
            }
        }
    }

    private fun saveCategory(
        transaction: TransactionEntity,
        categorized: CategorizedSmsTransaction
    ) {
        val category = repository.getCategories().firstOrNull {
            it.name == categorized.category &&
                it.kind == if (transaction.type.equals("DEBIT", true)) "EXPENSE" else "INCOME"
        }
        if (category == null) {
            Toast.makeText(this, "That category is no longer available.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        try {
            repository.assignCategory(transaction.id, category.id)
            repository.updateNote(transaction.id, categorized.note)
            completeAndShowNext(transaction.id)
        } catch (error: IllegalStateException) {
            Toast.makeText(this, error.message ?: "Could not save this transaction.", Toast.LENGTH_LONG).show()
            completeAndShowNext(transaction.id)
        }
    }

    private fun completeAndShowNext(transactionId: Long) {
        TransactionOverlayQueue.complete(this, transactionId)
        currentOverlay = loadNextOverlay()
        if (currentOverlay == null) finish()
    }

    private fun loadNextOverlay(): QueuedOverlay? {
        while (true) {
            val transactionId = TransactionOverlayQueue.currentOrNext(this) ?: return null
            val transaction = repository.getAll().firstOrNull { it.id == transactionId }
            if (transaction != null) {
                return QueuedOverlay(transaction, repository.getCategories())
            }
            TransactionOverlayQueue.complete(this, transactionId)
        }
    }

    private fun TransactionEntity.toOverlayDraft(): SmsTransactionDraft {
        val receivedAt = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            .format(Date(timestamp))
        return SmsTransactionDraft(
            sender = sender,
            receivedAt = receivedAt,
            direction = if (type.equals("DEBIT", true)) Direction.DEBIT else Direction.CREDIT,
            merchant = counterparty?.takeIf(String::isNotBlank)
                ?: merchant?.takeIf(String::isNotBlank)
                ?: "Bank transaction",
            upiReference = upiReference,
            accountLastFour = accountLast4,
            amount = amount / 100.0
        )
    }

    private data class QueuedOverlay(
        val transaction: TransactionEntity,
        val categories: List<CategoryEntity>
    )

}
