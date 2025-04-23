package com.example.cashly.ui.budget

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.cashly.R
import com.example.cashly.data.TransactionDatabase
import com.example.cashly.data.TransactionType
import com.example.cashly.data.UserPreferences
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.NumberFormat
import java.util.*

class BudgetFragment : Fragment() {
    private lateinit var transactionDatabase: TransactionDatabase
    private lateinit var userPreferences: UserPreferences
    private lateinit var budgetInput: TextInputEditText
    private lateinit var budgetInputLayout: TextInputLayout
    private lateinit var setBudgetButton: MaterialButton
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var progressText: TextView
    private lateinit var balanceText: TextView
    private lateinit var budgetChart: BarChart

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_budget, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionDatabase = TransactionDatabase(requireContext())
        userPreferences = UserPreferences(requireContext())
        
        budgetInput = view.findViewById(R.id.editTextBudget)
        budgetInputLayout = view.findViewById(R.id.textInputBudget)
        setBudgetButton = view.findViewById(R.id.buttonSetBudget)
        progressIndicator = view.findViewById(R.id.progressBudget)
        progressText = view.findViewById(R.id.textBudgetPercentage)
        balanceText = view.findViewById(R.id.textBudgetRemaining)
        budgetChart = view.findViewById(R.id.budgetChart)

        setBudgetButton.setOnClickListener {
            val budgetText = budgetInput.text?.toString() ?: ""
            val budget = budgetText.toDoubleOrNull()
            if (budget != null && budget > 0) {
                userPreferences.setMonthlyBudget(budget)
                updateBudgetInfo()
                setupBudgetChart()
                budgetInputLayout.error = null
                Toast.makeText(context, "Budget set successfully", Toast.LENGTH_SHORT).show()
            } else {
                budgetInputLayout.error = "Please enter a valid positive budget amount"
            }
        }

        setupBudgetChart()
        updateBudgetInfo()
    }

    private fun updateBudgetInfo() {
        val monthlyBudget = userPreferences.getMonthlyBudget()
        val currentExpenses = getCurrentMonthExpenses()
        val progress = if (monthlyBudget > 0) {
            ((currentExpenses / monthlyBudget) * 100).toInt().coerceIn(0, 100)
        } else 0

        progressIndicator.progress = progress
        progressText.text = getString(R.string.budget_progress_percentage, progress)

        val balance = monthlyBudget - currentExpenses
        val currencyFormat = NumberFormat.getCurrencyInstance()
        balanceText.text = getString(R.string.budget_remaining, currencyFormat.format(balance))

        if (monthlyBudget > 0) {
            budgetInput.setText(monthlyBudget.toString())
            budgetInputLayout.error = null
        } else {
            budgetInput.setText("")
        }
    }

    private fun getCurrentMonthExpenses(): Double {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        return transactionDatabase.getTransactions()
            .filter { transaction ->
                transaction.type == TransactionType.EXPENSE &&
                Calendar.getInstance().apply { time = transaction.date }.let { cal ->
                    cal.get(Calendar.MONTH) == currentMonth &&
                    cal.get(Calendar.YEAR) == currentYear
                }
            }
            .sumOf { it.amount }
    }

    private fun setupBudgetChart() {
        val monthlyBudget = userPreferences.getMonthlyBudget()
        val currentExpenses = getCurrentMonthExpenses()

        val entries = listOf(
            BarEntry(0f, monthlyBudget.toFloat()),
            BarEntry(1f, currentExpenses.toFloat())
        )

        val dataSet = BarDataSet(entries, "Budget vs Expenses")
        dataSet.colors = listOf(
            Color.rgb(76, 175, 80),  // Green for budget
            Color.rgb(244, 67, 54)   // Red for expenses
        )

        val barData = BarData(dataSet)
        barData.barWidth = 0.5f

        budgetChart.apply {
            data = barData
            description.isEnabled = false
            legend.isEnabled = true
            setDrawGridBackground(false)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(listOf("Budget", "Expenses"))
                granularity = 1f
                setDrawGridLines(false)
            }
            
            axisRight.isEnabled = false
            axisLeft.setDrawGridLines(false)
            
            animateY(1000)
            invalidate()
        }
    }

    override fun onResume() {
        super.onResume()
        updateBudgetInfo()
        setupBudgetChart()
    }
} 