package com.example.smsexpense.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smsexpense.data.CategoryEntity
import com.example.smsexpense.data.TransactionEntity

@Composable
internal fun UserIcon() {
    Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
        drawCircle(
            color = Color.White,
            radius = size.minDimension * 0.19f,
            center = Offset(size.width / 2f, size.height * 0.31f)
        )
        drawOval(
            color = Color.White,
            topLeft = Offset(size.width * 0.16f, size.height * 0.58f),
            size = Size(size.width * 0.68f, size.height * 0.5f)
        )
    }
}

@Composable
internal fun SettingsPage(
    darkTheme: Boolean,
    overlayAccessGranted: Boolean,
    card: Color,
    text: Color,
    muted: Color,
    onDarkThemeChange: (Boolean) -> Unit,
    onRequestOverlayAccess: () -> Unit,
    onManageCategories: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = card)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Appearance", color = text, fontWeight = FontWeight.SemiBold)
                    Text("Use a dark color scheme", color = muted, fontSize = 12.sp)
                }
                Switch(checked = darkTheme, onCheckedChange = onDarkThemeChange)
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onManageCategories),
            colors = CardDefaults.cardColors(containerColor = card)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Categories", color = text, fontWeight = FontWeight.SemiBold)
                    Text("Add, rename, and organize transaction categories", color = muted, fontSize = 12.sp)
                }
                Text("›", color = muted, fontSize = 24.sp)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = card)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Transaction overlay", color = text, fontWeight = FontWeight.SemiBold)
                Text(
                    if (overlayAccessGranted) {
                        "Enabled. Detected bank transaction SMS can appear over other apps."
                    } else {
                        "Allow this special access to show a review card when a bank transaction SMS arrives."
                    },
                    color = muted,
                    fontSize = 12.sp
                )
                if (!overlayAccessGranted) {
                    Button(onClick = onRequestOverlayAccess) {
                        Text("Enable transaction overlay")
                    }
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = card)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Privacy", color = text, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(5.dp))
                Text(
                    "SMS and transaction data stay on this device. The app does not use a network connection.",
                    color = muted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
internal fun CategoryManagementPage(
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    card: Color,
    text: Color,
    muted: Color,
    onAdd: (String, String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Boolean
) {
    var newName by remember { mutableStateOf("") }
    var newKind by remember { mutableStateOf("EXPENSE") }
    var message by remember { mutableStateOf<String?>(null) }
    val editNames = remember(categories) {
        androidx.compose.runtime.mutableStateMapOf<Long, String>().apply {
            categories.forEach { put(it.id, it.name) }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = card)) {
            Column(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("New category name") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(selected = newKind == "EXPENSE", onClick = { newKind = "EXPENSE" }, label = { Text("Expense") })
                    FilterChip(selected = newKind == "INCOME", onClick = { newKind = "INCOME" }, label = { Text("Income") })
                    Spacer(Modifier.weight(1f))
                    Button(onClick = {
                        val name = newName.trim()
                        when {
                            name.isBlank() -> message = "Category name cannot be empty."
                            categories.any { it.name.equals(name, true) } -> message = "That category already exists."
                            else -> {
                                onAdd(name, newKind)
                                newName = ""
                                message = "$name added."
                            }
                        }
                    }) { Text("Add") }
                }
                message?.let { Text(it, color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp) }
            }
        }
        Text("Categories", color = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories, key = { it.id }) { category ->
                val name = editNames[category.id] ?: category.name
                val inUse = transactions.any { it.categoryId == category.id }
                Card(colors = CardDefaults.cardColors(containerColor = card)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(11.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(category.icon, fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = name,
                                onValueChange = { editNames[category.id] = it },
                                modifier = Modifier.weight(1f),
                                label = { Text(category.kind.lowercase().replaceFirstChar(Char::uppercase)) },
                                singleLine = true
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (inUse) "In use" else if (category.isDefault) "Built-in" else "Custom",
                                color = muted,
                                fontSize = 10.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                enabled = name.trim().isNotBlank() && name.trim() != category.name,
                                onClick = {
                                    val normalized = name.trim()
                                    if (categories.any { it.id != category.id && it.name.equals(normalized, true) }) {
                                        message = "That category already exists."
                                    } else {
                                        onRename(category.id, normalized)
                                        message = "$normalized updated."
                                    }
                                }
                            ) { Text("Save") }
                            TextButton(
                                enabled = !category.isDefault && !inUse,
                                onClick = {
                                    message = if (onDelete(category.id)) "${category.name} deleted."
                                    else "Categories in use cannot be deleted."
                                }
                            ) { Text("Delete") }
                        }
                    }
                }
            }
        }
        Text(
            "Built-in or assigned categories cannot be deleted.",
            color = muted,
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}
