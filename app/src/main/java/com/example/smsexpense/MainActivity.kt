package com.example.smsexpense

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.smsexpense.data.TransactionRepositoryFactory
import com.example.smsexpense.sms.SmsInboxReader
import com.example.smsexpense.ui.HomeScreen
import com.example.smsexpense.util.AppLogger
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val context = LocalContext.current
                    AppLogger.log(context, "SmsTracker", "onCreate")
                    val repo = remember { TransactionRepositoryFactory.get(context) }
                    var txns by remember { mutableStateOf(repo.getAll()) }
                    var categories by remember { mutableStateOf(repo.getCategories()) }
                    val preferences = remember {
                        context.getSharedPreferences("home_screen_preferences", MODE_PRIVATE)
                    }
                    var darkTheme by remember {
                        mutableStateOf(preferences.getBoolean("dark_theme", false))
                    }
                    var overlayAccessGranted by remember {
                        mutableStateOf(Settings.canDrawOverlays(context))
                    }
                    var showOverlayPermissionPrompt by remember {
                        mutableStateOf(false)
                    }
                    var smsPermissionFlowFinished by remember {
                        mutableStateOf(false)
                    }
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner, repo) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                txns = repo.getAll()
                                categories = repo.getCategories()
                                overlayAccessGranted = Settings.canDrawOverlays(context)
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }
                    var exportContent by remember { mutableStateOf("") }
                    val exportLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.CreateDocument("text/csv")
                    ) { uri ->
                        if (uri != null) {
                            try {
                                val output = context.contentResolver.openOutputStream(uri)
                                    ?: throw IOException("Could not open the selected destination.")
                                output.bufferedWriter(Charsets.UTF_8).use { it.write(exportContent) }
                                Toast.makeText(context, "CSV exported.", Toast.LENGTH_SHORT).show()
                            } catch (error: IOException) {
                                AppLogger.log(context, "SmsTracker", "CSV export failed: ${error.message}")
                                Toast.makeText(context, "Could not export CSV.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    val perms = mutableListOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                    if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
                    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                        AppLogger.log(context, "SmsTracker", "Permissions result: $result")
                        smsPermissionFlowFinished = true
                        if (result.values.all { it }) {
                            AppLogger.log(context, "SmsTracker", "Permissions granted, start reading existing SMS")
                            Thread {
                                SmsInboxReader(repo).importOldSms(context)
                                (context as? android.app.Activity)?.runOnUiThread { txns = repo.getAll() }
                            }.start()
                        } else {
                            AppLogger.log(context, "SmsTracker", "Permissions denied")
                        }
                    }
                    LaunchedEffect(Unit) {
                        val need = perms.any { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
                        if (need) {
                            launcher.launch(perms.toTypedArray())
                        } else {
                            smsPermissionFlowFinished = true
                        }
                    }
                    LaunchedEffect(smsPermissionFlowFinished) {
                        if (
                            smsPermissionFlowFinished &&
                            !Settings.canDrawOverlays(context) &&
                            !preferences.getBoolean("overlay_permission_prompted", false)
                        ) {
                            showOverlayPermissionPrompt = true
                        }
                    }
                    if (showOverlayPermissionPrompt) {
                        AlertDialog(
                            onDismissRequest = {
                                preferences.edit().putBoolean("overlay_permission_prompted", true).apply()
                                showOverlayPermissionPrompt = false
                            },
                            title = { Text("Enable transaction overlay?") },
                            text = {
                                Text(
                                    "When a bank transaction SMS is detected, the app can show a review card over other apps. " +
                                        "Android will open its Display over other apps settings so you can allow or deny access."
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        preferences.edit().putBoolean("overlay_permission_prompted", true).apply()
                                        showOverlayPermissionPrompt = false
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Text("Enable")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        preferences.edit().putBoolean("overlay_permission_prompted", true).apply()
                                        showOverlayPermissionPrompt = false
                                    }
                                ) {
                                    Text("Not now")
                                }
                            }
                        )
                    }
                    HomeScreen(
                        transactions = txns,
                        categories = categories,
                        darkTheme = darkTheme,
                        overlayAccessGranted = overlayAccessGranted,
                        onDarkThemeChange = { darkTheme = it },
                        onRequestOverlayAccess = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        },
                        onAssignCategory = { transactionId, categoryId ->
                            repo.assignCategory(transactionId, categoryId)
                            txns = repo.getAll()
                        },
                        onUpdateNote = { transactionId, note ->
                            repo.updateNote(transactionId, note)
                            txns = repo.getAll()
                        },
                        onAddCategory = { name, kind ->
                            repo.addCategory(name, kind)
                            categories = repo.getCategories()
                        },
                        onRenameCategory = { categoryId, name ->
                            repo.renameCategory(categoryId, name)
                            categories = repo.getCategories()
                        },
                        onDeleteCategory = { categoryId ->
                            val deleted = repo.deleteCategory(categoryId)
                            categories = repo.getCategories()
                            deleted
                        },
                        onExportCsv = { selectedTransactions ->
                            val categoryNames = categories.associate { it.id to it.name }
                            exportContent = transactionsToCsv(selectedTransactions, categoryNames)
                            exportLauncher.launch("transactions-${LocalDate.now()}.csv")
                        }
                    )
                }
            }
        }
    }
}

private fun transactionsToCsv(
    transactions: List<com.example.smsexpense.data.TransactionEntity>,
    categories: Map<Long, String>
): String = buildString {
    appendLine("Date,Sender,Counterparty,Merchant,Type,Amount INR,Category,Account last 4,UPI reference,Balance after INR,Note,SMS")
    transactions.forEach { transaction ->
        val values = listOf(
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(transaction.timestamp)),
            transaction.sender,
            transaction.counterparty.orEmpty(),
            transaction.merchant.orEmpty(),
            transaction.type,
            BigDecimal.valueOf(transaction.amount, 2).toPlainString(),
            transaction.categoryId?.let(categories::get).orEmpty(),
            transaction.accountLast4.orEmpty(),
            transaction.upiReference.orEmpty(),
            transaction.balanceAfter?.let { BigDecimal.valueOf(it, 2).toPlainString() }.orEmpty(),
            transaction.note.orEmpty(),
            transaction.rawBody
        )
        appendLine(values.joinToString(",") { value ->
            "\"${value.replace("\"", "\"\"")}\""
        })
    }
}
