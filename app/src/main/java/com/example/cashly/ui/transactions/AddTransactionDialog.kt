package com.example.cashly.ui.transactions

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import com.example.cashly.R
import com.example.cashly.data.Transaction
import com.example.cashly.data.TransactionType
import com.google.android.material.textfield.TextInputLayout
import java.util.Date

class AddTransactionDialog : DialogFragment() {
    private var onSave: ((Transaction) -> Unit)? = null
    private var existingTransaction: Transaction? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_add_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val descriptionEditText = view.findViewById<EditText>(R.id.description_edit_text)
        val amountEditText = view.findViewById<EditText>(R.id.amount_edit_text)
        val categorySpinner = view.findViewById<Spinner>(R.id.category_spinner)
        val incomeRadioButton = view.findViewById<RadioButton>(R.id.income_radio)
        val saveButton = view.findViewById<Button>(R.id.save_button)
        val cancelButton = view.findViewById<Button>(R.id.cancel_button)

        // Populate category spinner
        val categories = resources.getStringArray(R.array.transaction_categories)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        // Set existing values if editing
        existingTransaction?.let { transaction ->
            descriptionEditText.setText(transaction.description)
            amountEditText.setText(transaction.amount.toString())
            categorySpinner.setSelection(categories.indexOf(transaction.category))
            incomeRadioButton.isChecked = transaction.type == TransactionType.INCOME
        }

        saveButton.setOnClickListener {
            val description = descriptionEditText.text.toString()
            val amountText = amountEditText.text.toString()
            val category = categorySpinner.selectedItem.toString()
            val type = if (incomeRadioButton.isChecked) TransactionType.INCOME else TransactionType.EXPENSE

            // Validate amount
            if (amountText.isBlank()) {
                amountEditText.error = "Please enter an amount"
                amountEditText.requestFocus()
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                amountEditText.error = "Please enter a valid amount"
                amountEditText.requestFocus()
                return@setOnClickListener
            }

            // Validate description
            if (description.isBlank()) {
                descriptionEditText.error = "Please enter a description"
                descriptionEditText.requestFocus()
                return@setOnClickListener
            }

            // Validate category
            if (category.isBlank()) {
                (categorySpinner.parent as? TextInputLayout)?.error = "Please select a category"
                categorySpinner.requestFocus()
                return@setOnClickListener
            }

            val transaction = existingTransaction?.copy(
                amount = amount,
                description = description,
                category = category,
                type = type
            ) ?: Transaction(
                amount = amount,
                description = description,
                category = category,
                date = Date(),
                type = type
            )
            onSave?.invoke(transaction)
            dismiss()
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        fun newInstance(onSave: (Transaction) -> Unit): AddTransactionDialog {
            return AddTransactionDialog().apply {
                this.onSave = onSave
            }
        }

        fun newInstance(transaction: Transaction, onSave: (Transaction) -> Unit): AddTransactionDialog {
            return AddTransactionDialog().apply {
                this.existingTransaction = transaction
                this.onSave = onSave
            }
        }
    }
} 