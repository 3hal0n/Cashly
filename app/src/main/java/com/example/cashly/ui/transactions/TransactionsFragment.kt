package com.example.cashly.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cashly.R
import com.example.cashly.data.Transaction
import com.example.cashly.data.TransactionDatabase
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TransactionsFragment : Fragment() {
    private lateinit var transactionDatabase: TransactionDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionsAdapter

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
        recyclerView = view.findViewById(R.id.recycler_view)
        val fab = view.findViewById<FloatingActionButton>(R.id.fab_add_transaction)

        adapter = TransactionsAdapter(
            transactions = transactionDatabase.getTransactions(),
            onEditClick = { transaction -> showEditTransactionDialog(transaction) },
            onDeleteClick = { transaction -> showDeleteTransactionDialog(transaction) }
        )
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        fab.setOnClickListener {
            showAddTransactionDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.updateTransactions(transactionDatabase.getTransactions())
    }

    private fun showAddTransactionDialog() {
        val dialog = AddTransactionDialog.newInstance { transaction ->
            transactionDatabase.saveTransaction(transaction)
            adapter.updateTransactions(transactionDatabase.getTransactions())
        }
        dialog.show(childFragmentManager, "AddTransactionDialog")
    }

    private fun showEditTransactionDialog(transaction: Transaction) {
        val dialog = AddTransactionDialog.newInstance(
            transaction = transaction,
            onSave = { updatedTransaction ->
                transactionDatabase.updateTransaction(updatedTransaction)
                adapter.updateTransactions(transactionDatabase.getTransactions())
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
                adapter.updateTransactions(transactionDatabase.getTransactions())
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
} 