package com.example.smsexpense.data

data class CategoryEntity(
    val id: Long = 0,
    val name: String,
    val kind: String,
    val icon: String,
    val isDefault: Boolean = false
)