package com.example.cashly.ui.transactions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import com.example.cashly.R
import com.example.cashly.data.Transaction
import com.example.cashly.data.TransactionType
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionsAdapter(
    private var transactions: List<Transaction>,
    private val onEditClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionsAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val categoryIconImageView: ImageView = view.findViewById(R.id.categoryIcon)
        val descriptionTextView: TextView = view.findViewById(R.id.transactionDescription)
        val amountTextView: TextView = view.findViewById(R.id.transactionAmount)
        val categoryTextView: TextView = view.findViewById(R.id.transactionCategory)
        val dateTextView: TextView = view.findViewById(R.id.transactionDate)
        val editButton: MaterialButton = view.findViewById(R.id.editButton)
        val deleteButton: MaterialButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions[position]

        holder.descriptionTextView.text = transaction.description
        holder.categoryTextView.text = transaction.category
        holder.dateTextView.text = dateFormat.format(transaction.date)
        
        holder.categoryIconImageView.setImageResource(getCategoryIconResId(transaction.category))

        val amount = when (transaction.type) {
            TransactionType.INCOME -> transaction.amount
            TransactionType.EXPENSE -> -transaction.amount
        }
        
        holder.amountTextView.apply {
            text = currencyFormat.format(amount)
            setTextColor(context.getColor(
                when (transaction.type) {
                    TransactionType.INCOME -> R.color.income_green
                    TransactionType.EXPENSE -> R.color.expense_red
                }
            ))
        }

        holder.editButton.setOnClickListener { onEditClick(transaction) }
        holder.deleteButton.setOnClickListener { onDeleteClick(transaction) }
    }

    override fun getItemCount() = transactions.size

    fun updateTransactions(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }

    @DrawableRes
    private fun getCategoryIconResId(category: String): Int {
        return when (category.lowercase(Locale.ROOT)) {
            "food" -> R.drawable.food
            "transport" -> R.drawable.transport
            "bills" -> R.drawable.bills
            "entertainment" -> R.drawable.entertainment
            "shopping" -> R.drawable.shopping
            "healthcare" -> R.drawable.healthcare
            "education" -> R.drawable.ic_category
            "housing" -> R.drawable.ic_category
            "salary" -> R.drawable.salary
            "investment" -> R.drawable.investment
            "bonus" -> R.drawable.bonus
            "other income" -> R.drawable.otherincome
            "other expense" -> R.drawable.ic_expense
            else -> R.drawable.ic_category
        }
    }
} 