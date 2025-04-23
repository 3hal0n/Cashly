package com.example.cashly.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Date

class TransactionDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "cashly.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_TRANSACTIONS = "transactions"
        private const val COLUMN_ID = "id"
        private const val COLUMN_AMOUNT = "amount"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_CATEGORY = "category"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_DATE = "date"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTransactionsTable = """
            CREATE TABLE $TABLE_TRANSACTIONS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_AMOUNT REAL NOT NULL,
                $COLUMN_DESCRIPTION TEXT NOT NULL,
                $COLUMN_CATEGORY TEXT NOT NULL,
                $COLUMN_TYPE TEXT NOT NULL,
                $COLUMN_DATE INTEGER NOT NULL
            )
        """.trimIndent()

        db.execSQL(createTransactionsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TRANSACTIONS")
        onCreate(db)
    }

    fun addTransaction(transaction: Transaction): Long {
        return saveTransaction(transaction)
    }

    fun getTransactions(): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val cursor = readableDatabase.query(
            TABLE_TRANSACTIONS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_DATE DESC"
        )

        with(cursor) {
            while (moveToNext()) {
                val transaction = Transaction(
                    id = getLong(getColumnIndexOrThrow(COLUMN_ID)),
                    amount = getDouble(getColumnIndexOrThrow(COLUMN_AMOUNT)),
                    description = getString(getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                    category = getString(getColumnIndexOrThrow(COLUMN_CATEGORY)),
                    type = TransactionType.valueOf(getString(getColumnIndexOrThrow(COLUMN_TYPE))),
                    date = Date(getLong(getColumnIndexOrThrow(COLUMN_DATE)))
                )
                transactions.add(transaction)
            }
        }
        cursor.close()
        return transactions
    }

    fun deleteTransaction(id: Long): Int {
        return writableDatabase.delete(
            TABLE_TRANSACTIONS,
            "$COLUMN_ID = ?",
            arrayOf(id.toString())
        )
    }

    fun saveTransaction(transaction: Transaction): Long {
        val values = ContentValues().apply {
            put(COLUMN_AMOUNT, transaction.amount)
            put(COLUMN_DESCRIPTION, transaction.description)
            put(COLUMN_CATEGORY, transaction.category)
            put(COLUMN_TYPE, transaction.type.name)
            put(COLUMN_DATE, transaction.date.time)
        }

        return if (transaction.id == 0L) {
            writableDatabase.insert(TABLE_TRANSACTIONS, null, values)
        } else {
            values.put(COLUMN_ID, transaction.id)
            writableDatabase.update(
                TABLE_TRANSACTIONS,
                values,
                "$COLUMN_ID = ?",
                arrayOf(transaction.id.toString())
            ).toLong()
            transaction.id
        }
    }

    fun updateTransaction(transaction: Transaction) {
        saveTransaction(transaction)
    }

    fun deleteTransaction(transaction: Transaction) {
        deleteTransaction(transaction.id)
    }

    fun clearAllTransactions() {
        writableDatabase.delete(TABLE_TRANSACTIONS, null, null)
    }

    private fun saveTransactions(transactions: List<Transaction>) {
        writableDatabase.beginTransaction()
        try {
            writableDatabase.delete(TABLE_TRANSACTIONS, null, null)
            transactions.forEach { transaction ->
                saveTransaction(transaction)
            }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }
} 