package com.example.cashly.ui.analytics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.cashly.R
import com.example.cashly.data.Transaction
import com.example.cashly.data.TransactionDatabase
import com.example.cashly.data.TransactionType
import com.example.cashly.data.UserPreferences
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsFragment : Fragment() {
    private lateinit var transactionDatabase: TransactionDatabase
    private lateinit var userPreferences: UserPreferences
    private lateinit var incomeChart: PieChart
    private lateinit var incomeTrendChart: LineChart
    private lateinit var expenseTrendChart: LineChart
    private lateinit var totalExpensesText: TextView
    private lateinit var totalIncomeText: TextView
    private lateinit var netBalanceText: TextView
    private lateinit var budgetProgressText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_analytics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionDatabase = TransactionDatabase(requireContext())
        userPreferences = UserPreferences(requireContext())
        
        incomeChart = view.findViewById(R.id.incomeDistributionChart)
        incomeTrendChart = view.findViewById(R.id.incomeTrendChart)
        expenseTrendChart = view.findViewById(R.id.expenseTrendChart)
        totalExpensesText = view.findViewById(R.id.textTotalExpenses)
        totalIncomeText = view.findViewById(R.id.textTotalIncome)
        netBalanceText = view.findViewById(R.id.textNetBalance)
        budgetProgressText = view.findViewById(R.id.textBudgetProgress)

        setupIncomeDistributionChart()
        setupIncomeTrendChart()
        setupExpenseTrendChart()
        updateAnalytics()
    }

    override fun onResume() {
        super.onResume()
        updateAnalytics()
    }

    private fun setupIncomeDistributionChart() {
        val transactions = transactionDatabase.getTransactions()
        val incomeByCategory = transactions
            .filter { it.type == TransactionType.INCOME }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { transaction -> transaction.amount } }

        val entries = incomeByCategory.map { (category, amount) ->
            PieEntry(amount.toFloat(), category)
        }.sortedWith(compareBy { -it.value })

        val dataSet = PieDataSet(entries, "Income Distribution")
        dataSet.colors = listOf(
            Color.rgb(64, 89, 128),
            Color.rgb(149, 165, 124),
            Color.rgb(217, 184, 162),
            Color.rgb(191, 134, 134),
            Color.rgb(179, 48, 80)
        )
        
        val pieData = PieData(dataSet)
        incomeChart.apply {
            data = pieData
            description.isEnabled = false
            setUsePercentValues(true)
            setEntryLabelColor(Color.WHITE)
            animateY(1000)
            invalidate()
        }
    }

    private fun setupIncomeTrendChart() {
        setupTrendChart(incomeTrendChart, TransactionType.INCOME, "Income Trend")
    }

    private fun setupExpenseTrendChart() {
        setupTrendChart(expenseTrendChart, TransactionType.EXPENSE, "Expense Trend")
    }

    private fun setupTrendChart(chart: LineChart, type: TransactionType, label: String) {
        val transactions = transactionDatabase.getTransactions()
            .filter { it.type == type }
            .sortedBy { it.date }

        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        val entries = transactions.mapIndexed { index, transaction ->
            Entry(index.toFloat(), transaction.amount.toFloat())
        }

        val dataSet = LineDataSet(entries, label)
        dataSet.apply {
            color = if (type == TransactionType.INCOME) 
                Color.rgb(76, 175, 80) else Color.rgb(244, 67, 54)
            setCircleColor(color)
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
        }

        val xAxis = chart.xAxis
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = IndexAxisValueFormatter(
                transactions.map { dateFormat.format(it.date) }
            )
            labelRotationAngle = -45f
            granularity = 1f
        }

        chart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            axisRight.isEnabled = false
            legend.isEnabled = true
            animateX(1000)
            invalidate()
        }
    }

    private fun updateAnalytics() {
        val currentMonthTransactions = getCurrentMonthTransactions()
        val expenses = currentMonthTransactions.filter { it.type == TransactionType.EXPENSE }
        val income = currentMonthTransactions.filter { it.type == TransactionType.INCOME }

        // Update pie chart
        val entries = expenses.groupBy { it.category }
            .map { (category, transactions) ->
                PieEntry(transactions.sumOf { it.amount }.toFloat(), category)
            }
            .sortedWith(compareBy { -it.value })

        val dataSet = PieDataSet(entries, "Expenses by Category")
        dataSet.colors = getCategoryColors(entries.size)
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f
        dataSet.valueFormatter = PercentFormatter(incomeChart)

        incomeChart.data = PieData(dataSet)
        incomeChart.invalidate()

        // Update summary texts
        val currencyFormat = NumberFormat.getCurrencyInstance()
        val totalExpenses = expenses.sumOf { it.amount }
        val totalIncome = income.sumOf { it.amount }
        val netBalance = totalIncome - totalExpenses

        totalExpensesText.text = currencyFormat.format(totalExpenses)
        totalIncomeText.text = currencyFormat.format(totalIncome)
        netBalanceText.text = currencyFormat.format(netBalance)

        // Update budget progress
        val monthlyBudget = userPreferences.getMonthlyBudget()
        if (monthlyBudget > 0) {
            val percentage = (totalExpenses / monthlyBudget) * 100
            budgetProgressText.text = "Budget Usage: ${String.format("%.1f", percentage)}%"
            budgetProgressText.setTextColor(
                when {
                    percentage >= 100 -> Color.RED
                    percentage >= 80 -> Color.rgb(255, 165, 0) // Orange
                    else -> Color.GREEN
                }
            )
        } else {
            budgetProgressText.text = "No budget set"
            budgetProgressText.setTextColor(Color.GRAY)
        }
    }

    private fun getCurrentMonthTransactions(): List<Transaction> {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        return transactionDatabase.getTransactions().filter { transaction ->
            calendar.time = transaction.date
            calendar.get(Calendar.MONTH) == currentMonth && 
            calendar.get(Calendar.YEAR) == currentYear
        }
    }

    private fun getCategoryColors(count: Int): List<Int> {
        val baseColors = listOf(
            Color.rgb(244, 67, 54),   // Red
            Color.rgb(255, 152, 0),   // Orange
            Color.rgb(76, 175, 80),   // Green
            Color.rgb(33, 150, 243),  // Blue
            Color.rgb(156, 39, 176)   // Purple
        )
        return if (count <= baseColors.size) {
            baseColors.take(count)
        } else {
            baseColors + List(count - baseColors.size) { index ->
                Color.rgb(
                    (Math.random() * 255).toInt(),
                    (Math.random() * 255).toInt(),
                    (Math.random() * 255).toInt()
                )
            }
        }
    }
} 