package com.example.cashly.data

import java.util.Date

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val description: String,
    val category: String,
    val type: TransactionType,
    val date: Date
) 