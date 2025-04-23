package com.example.cashly.data

import android.content.Context
import com.google.gson.Gson
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.Date

class BackupManager(private val context: Context) {
    private val gson = Gson()
    private val userPreferences = UserPreferences(context)
    private val backupFile: File
        get() = File(
            context.filesDir,
            "cashly_backup.json"
        )

    fun createBackup(transactions: List<Transaction>): Boolean {
        return try {
            val backupData = BackupData(
                transactions = transactions,
                monthlyBudget = userPreferences.getMonthlyBudget(),
                currency = userPreferences.getCurrency(),
                timestamp = Date()
            )
            FileWriter(backupFile).use { writer ->
                gson.toJson(backupData, writer)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun restoreBackup(): BackupRestoreResult? {
        return try {
            if (!backupFile.exists()) {
                return null
            }
            FileReader(backupFile).use { reader ->
                val backupData = gson.fromJson(reader, BackupData::class.java)
                // Restore user preferences
                userPreferences.setMonthlyBudget(backupData.monthlyBudget)
                userPreferences.setCurrency(backupData.currency)
                
                BackupRestoreResult(
                    transactions = backupData.transactions,
                    monthlyBudget = backupData.monthlyBudget,
                    currency = backupData.currency
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun hasBackup(): Boolean {
        return backupFile.exists()
    }

    fun getBackupFilePath(): String {
        return backupFile.absolutePath
    }

    private data class BackupData(
        val transactions: List<Transaction>,
        val monthlyBudget: Double,
        val currency: String,
        val timestamp: Date
    )

    data class BackupRestoreResult(
        val transactions: List<Transaction>,
        val monthlyBudget: Double,
        val currency: String
    )
} 
 