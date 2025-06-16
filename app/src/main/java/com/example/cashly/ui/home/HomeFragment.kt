package com.example.cashly.ui.home

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cashly.R
import com.example.cashly.data.Transaction
import com.example.cashly.data.TransactionDatabase
import com.example.cashly.data.TransactionType
import com.example.cashly.data.UserPreferences
import com.example.cashly.ui.transactions.TransactionsAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.example.cashly.notifications.NotificationManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.NumberFormat
import java.util.Currency

class HomeFragment : Fragment() {
    private lateinit var transactionDatabase: TransactionDatabase
    private lateinit var userPreferences: UserPreferences
    private lateinit var notificationManager: NotificationManager
    private lateinit var amountInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var categorySpinner: AutoCompleteTextView
    private lateinit var dateButton: MaterialButton
    private lateinit var addExpenseButton: MaterialButton
    private lateinit var addIncomeButton: MaterialButton
    private lateinit var recentTransactionsList: RecyclerView
    private var transactionAdapter: TransactionsAdapter? = null
    private var selectedDate: Date = Date()

    // Budget threshold constants
    private companion object {
        const val BUDGET_WARNING_THRESHOLD = 0.8 // 80%
        const val BUDGET_CRITICAL_THRESHOLD = 0.9 // 90%
        const val MAX_PERCENTAGE = 100
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeComponents(view)
        setupCategorySpinner()
        setupDateButton()
        setupRecyclerView()
        setupButtonListeners()
        loadRecentTransactions()
    }

    override fun onResume() {
        super.onResume()
        loadRecentTransactions()
    }

    private fun initializeComponents(view: View) {
        transactionDatabase = TransactionDatabase(requireContext())
        userPreferences = UserPreferences(requireContext())
        notificationManager = NotificationManager(requireContext())

        amountInput = view.findViewById(R.id.amountInput)
        descriptionInput = view.findViewById(R.id.descriptionInput)
        categorySpinner = view.findViewById(R.id.categorySpinner)
        dateButton = view.findViewById(R.id.dateButton)
        addExpenseButton = view.findViewById(R.id.addExpenseButton)
        addIncomeButton = view.findViewById(R.id.addIncomeButton)
        recentTransactionsList = view.findViewById(R.id.recentTransactionsList)
    }

    private fun setupCategorySpinner() {
        val categories = resources.getStringArray(R.array.transaction_categories)
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        )
        categorySpinner.setAdapter(categoryAdapter)
    }

    private fun setupDateButton() {
        updateDateButtonText()
        dateButton.setOnClickListener { showDatePicker() }
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionsAdapter(
            emptyList(),
            userPreferences,
            onEditClick = { transaction -> showEditTransactionDialog(transaction) },
            onDeleteClick = { transaction -> showDeleteTransactionDialog(transaction) }
        )
        recentTransactionsList.layoutManager = LinearLayoutManager(context)
        recentTransactionsList.adapter = transactionAdapter
    }

    private fun setupButtonListeners() {
        addExpenseButton.setOnClickListener { addTransaction(TransactionType.EXPENSE) }
        addIncomeButton.setOnClickListener { addTransaction(TransactionType.INCOME) }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { time = selectedDate }

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDate = calendar.time
                updateDateButtonText()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateButtonText() {
        dateButton.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate)
    }

    private fun addTransaction(type: TransactionType) {
        val amount = amountInput.text?.toString()?.toDoubleOrNull() ?: 0.0
        val description = descriptionInput.text?.toString() ?: ""
        val category = categorySpinner.text?.toString() ?: ""

        if (!validateInputs(amount, description, category)) return

        val transaction = Transaction(
            amount = amount,
            description = description,
            category = category,
            type = type,
            date = selectedDate
        )

        transactionDatabase.addTransaction(transaction)
        clearInputs()
        loadRecentTransactions()

        if (type == TransactionType.EXPENSE) {
            checkBudgetLimits(amount)
        }
    }

    private fun validateInputs(amount: Double, description: String, category: String): Boolean {
        var isValid = true

        when {
            amount <= 0 -> {
                amountInput.error = "Please enter a valid amount"
                amountInput.requestFocus()
                isValid = false
            }
            description.isBlank() -> {
                descriptionInput.error = "Please enter a description"
                descriptionInput.requestFocus()
                isValid = false
            }
            category.isBlank() -> {
                categorySpinner.error = "Please select a category"
                categorySpinner.requestFocus()
                isValid = false
            }
        }

        return isValid
    }

    private fun checkBudgetLimits(newExpense: Double) {
        val monthlyBudget = userPreferences.getMonthlyBudget()
        if (monthlyBudget <= 0) return

        val currentMonthExpenses = getCurrentMonthExpenses()
        val totalAfterTransaction = currentMonthExpenses + newExpense
        val percentageUsed = (totalAfterTransaction / monthlyBudget).coerceAtMost(1.0) * 100
        val formattedPercentage = "%.1f".format(percentageUsed)

        when {
            totalAfterTransaction > monthlyBudget -> {
                notifyBudgetStatus(
                    "Budget Exceeded!",
                    "You've used $formattedPercentage% of your budget",
                    formattedPercentage
                )
            }
            percentageUsed >= (BUDGET_CRITICAL_THRESHOLD * 100) -> {
                notifyBudgetStatus(
                    "Budget Critical!",
                    "You've used $formattedPercentage% of your budget",
                    formattedPercentage
                )
            }
            percentageUsed >= (BUDGET_WARNING_THRESHOLD * 100) -> {
                notifyBudgetStatus(
                    "Budget Warning",
                    "You've used $formattedPercentage% of your budget",
                    formattedPercentage
                )
            }
        }
    }

    private fun getCurrentMonthExpenses(): Double {
        return transactionDatabase.getTransactions()
            .filter { transaction ->
                transaction.type == TransactionType.EXPENSE &&
                        isSameMonth(transaction.date, Date())
            }
            .sumOf { it.amount }
    }

    private fun notifyBudgetStatus(title: String, message: String, percentage: String) {
        notificationManager.showBudgetAlert(percentage)
        showBudgetAlertDialog(title, message)
    }

    private fun isSameMonth(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }

    private fun clearInputs() {
        amountInput.text?.clear()
        descriptionInput.text?.clear()
        categorySpinner.text?.clear()
        selectedDate = Date()
        updateDateButtonText()
        amountInput.requestFocus()
    }

    private fun loadRecentTransactions() {
        val transactions = transactionDatabase.getTransactions()
            .sortedByDescending { it.date }
            .take(10)
        transactionAdapter?.updateTransactions(transactions)
    }

    private fun showBudgetAlertDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showEditTransactionDialog(transaction: Transaction) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_transaction, null)

        with(dialogView) {
            val currencyFormat = NumberFormat.getCurrencyInstance(userPreferences.getCurrencyLocale())
            findViewById<TextInputEditText>(R.id.editAmountInput).setText(currencyFormat.format(transaction.amount))
            findViewById<TextInputEditText>(R.id.editDescriptionInput).setText(transaction.description)
            findViewById<AutoCompleteTextView>(R.id.editCategorySpinner).setText(transaction.category)

            val typeSwitch = findViewById<MaterialSwitch>(R.id.typeSwitch).apply {
                isChecked = transaction.type == TransactionType.INCOME
                text = if (isChecked) "Income" else "Expense"
                setOnCheckedChangeListener { buttonView, isChecked ->
                    buttonView.text = if (isChecked) "Income" else "Expense"
                }
            }

            // Setup category adapter
            val categories = resources.getStringArray(R.array.transaction_categories)
            val categoryAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categories
            )
            findViewById<AutoCompleteTextView>(R.id.editCategorySpinner).setAdapter(categoryAdapter)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Transaction")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                with(dialogView) {
                    val newAmount = findViewById<TextInputEditText>(R.id.editAmountInput)
                        .text?.toString()?.toDoubleOrNull() ?: return@with
                    val newDescription = findViewById<TextInputEditText>(R.id.editDescriptionInput)
                        .text?.toString() ?: return@with
                    val newCategory = findViewById<AutoCompleteTextView>(R.id.editCategorySpinner)
                        .text?.toString() ?: return@with
                    val newType = if (findViewById<MaterialSwitch>(R.id.typeSwitch).isChecked) 
                        TransactionType.INCOME else TransactionType.EXPENSE

                    if (validateInputs(newAmount, newDescription, newCategory)) {
                        val updatedTransaction = Transaction(
                            id = transaction.id,
                            amount = newAmount,
                            description = newDescription,
                            category = newCategory,
                            type = newType,
                            date = transaction.date
                        )
                        transactionDatabase.updateTransaction(updatedTransaction)
                        loadRecentTransactions()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteTransactionDialog(transaction: Transaction) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                transactionDatabase.deleteTransaction(transaction)
                loadRecentTransactions()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}