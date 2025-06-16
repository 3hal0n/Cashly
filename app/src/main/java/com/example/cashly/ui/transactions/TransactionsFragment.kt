package com.example.cashly.ui.transactions

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cashly.R
import com.example.cashly.data.Transaction
import com.example.cashly.data.TransactionDatabase
import com.example.cashly.data.UserPreferences
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class TransactionsFragment : Fragment() {
    private lateinit var transactionDatabase: TransactionDatabase
    private lateinit var userPreferences: UserPreferences
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionsAdapter
    private lateinit var searchEditText: TextInputEditText
    private lateinit var exportButton: MaterialButton
    private var allTransactions: List<Transaction> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionDatabase = TransactionDatabase(requireContext())
        userPreferences = UserPreferences(requireContext())
        recyclerView = view.findViewById(R.id.recycler_view)
        searchEditText = view.findViewById(R.id.searchEditText)
        exportButton = view.findViewById(R.id.exportButton)
        val fab = view.findViewById<FloatingActionButton>(R.id.fab_add_transaction)

        setupRecyclerView()
        setupSearch()
        setupExportButton()

        fab.setOnClickListener {
            showAddTransactionDialog()
        }
    }

    private fun setupRecyclerView() {
        allTransactions = transactionDatabase.getTransactions()
        adapter = TransactionsAdapter(
            transactions = allTransactions,
            userPreferences = userPreferences,
            onEditClick = { transaction -> showEditTransactionDialog(transaction) },
            onDeleteClick = { transaction -> showDeleteTransactionDialog(transaction) }
        )
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterTransactions(s?.toString() ?: "")
            }
        })
    }

    private fun setupExportButton() {
        exportButton.setOnClickListener {
            exportTransactions()
        }
    }

    private fun filterTransactions(query: String) {
        val filteredList = if (query.isEmpty()) {
            allTransactions
        } else {
            allTransactions.filter { transaction ->
                transaction.description.contains(query, ignoreCase = true) ||
                transaction.category.contains(query, ignoreCase = true) ||
                transaction.amount.toString().contains(query, ignoreCase = true)
            }
        }
        adapter.updateTransactions(filteredList)
    }

    private fun exportTransactions() {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
            val fileName = "transactions_${dateFormat.format(Date())}.csv"
            val file = File(requireContext().filesDir, fileName)
            
            FileWriter(file).use { writer ->
                // Write CSV header
                writer.append("Date,Description,Category,Type,Amount\n")
                
                // Write transactions
                allTransactions.forEach { transaction ->
                    writer.append("${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(transaction.date)},")
                    writer.append("\"${transaction.description.replace("\"", "\"\"")}\",")
                    writer.append("\"${transaction.category.replace("\"", "\"\"")}\",")
                    writer.append("${transaction.type},")
                    writer.append("${transaction.amount}\n")
                }
            }

            // Share the file
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(intent, "Export Transactions"))
        } catch (e: Exception) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Export Failed")
                .setMessage("Failed to export transactions: ${e.message}")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        allTransactions = transactionDatabase.getTransactions()
        filterTransactions(searchEditText.text?.toString() ?: "")
    }

    private fun showAddTransactionDialog() {
        val dialog = AddTransactionDialog.newInstance { transaction ->
            transactionDatabase.saveTransaction(transaction)
            allTransactions = transactionDatabase.getTransactions()
            filterTransactions(searchEditText.text?.toString() ?: "")
        }
        dialog.show(childFragmentManager, "AddTransactionDialog")
    }

    private fun showEditTransactionDialog(transaction: Transaction) {
        val dialog = AddTransactionDialog.newInstance(
            transaction = transaction,
            onSave = { updatedTransaction ->
                transactionDatabase.updateTransaction(updatedTransaction)
                allTransactions = transactionDatabase.getTransactions()
                filterTransactions(searchEditText.text?.toString() ?: "")
            }
        )
        dialog.show(childFragmentManager, "EditTransactionDialog")
    }

    private fun showDeleteTransactionDialog(transaction: Transaction) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_transaction)
            .setMessage(R.string.delete_transaction_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                transactionDatabase.deleteTransaction(transaction)
                allTransactions = transactionDatabase.getTransactions()
                filterTransactions(searchEditText.text?.toString() ?: "")
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}