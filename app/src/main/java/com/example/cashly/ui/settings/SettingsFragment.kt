package com.example.cashly.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.cashly.R
import com.example.cashly.data.BackupManager
import com.example.cashly.data.Transaction
import com.example.cashly.data.TransactionDatabase
import com.example.cashly.data.UserPreferences
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import java.util.Currency
import java.util.Date
import java.util.Locale

class SettingsFragment : Fragment() {
    private lateinit var transactionDatabase: TransactionDatabase
    private lateinit var backupManager: BackupManager
    private lateinit var userPreferences: UserPreferences
    private lateinit var currencySpinner: Spinner
    private lateinit var saveButton: Button
    private lateinit var backupButton: Button
    private lateinit var restoreButton: Button
    private lateinit var changePinButton: MaterialButton
    private lateinit var darkModeSwitch: MaterialSwitch

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            createBackup()
        } else {
            Toast.makeText(context, "Storage permission is required for backup", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionDatabase = TransactionDatabase(requireContext())
        backupManager = BackupManager(requireContext())
        userPreferences = UserPreferences(requireContext())
        
        currencySpinner = view.findViewById(R.id.currency_spinner)
        saveButton = view.findViewById(R.id.save_button)
        backupButton = view.findViewById(R.id.backup_button)
        restoreButton = view.findViewById(R.id.restore_button)
        changePinButton = view.findViewById(R.id.changePinButton)
        darkModeSwitch = view.findViewById(R.id.darkModeSwitch)

        setupCurrencySpinner()
        setupPinChangeButton()
        setupDarkModeSwitch()

        saveButton.setOnClickListener {
            saveCurrencySettings()
        }

        backupButton.setOnClickListener {
            checkStoragePermissionAndBackup()
        }

        restoreButton.setOnClickListener {
            val restoreResult = backupManager.restoreBackup()
            if (restoreResult != null) {
                // Restore transactions
                transactionDatabase.clearAllTransactions()
                restoreResult.transactions.forEach { backedUpTransaction ->
                    // Create a new transaction object without the ID to ensure insertion
                    val newTransaction = Transaction(
                        // id = 0L, // Let the database auto-generate the ID
                        amount = backedUpTransaction.amount,
                        description = backedUpTransaction.description,
                        category = backedUpTransaction.category,
                        type = backedUpTransaction.type,
                        date = backedUpTransaction.date
                    )
                    transactionDatabase.addTransaction(newTransaction)
                }
                
                // Update UI to reflect restored settings
                updateCurrencySpinner(restoreResult.currency)
                
                Toast.makeText(context, "Backup restored successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "No backup found", Toast.LENGTH_SHORT).show()
            }
        }

        restoreButton.isEnabled = backupManager.hasBackup()
    }

    private fun setupDarkModeSwitch() {
        darkModeSwitch.isChecked = userPreferences.getTheme() == "dark"
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            val theme = if (isChecked) "dark" else "light"
            userPreferences.setTheme(theme)
            applyTheme(theme)
        }
    }

    private fun applyTheme(theme: String) {
        when (theme) {
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun updateCurrencySpinner(currency: String) {
        val position = (currencySpinner.adapter as ArrayAdapter<String>).getPosition(
            currencySpinner.adapter.getItem(0).toString().split(" - ").first { it == currency }
        )
        if (position != -1) {
            currencySpinner.setSelection(position)
        }
    }

    private fun checkStoragePermissionAndBackup() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                createBackup()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun createBackup() {
        val success = backupManager.createBackup(transactionDatabase.getTransactions())
        if (success) {
            val backupPath = backupManager.getBackupFilePath()
            Toast.makeText(
                context,
                "Backup created successfully at: $backupPath",
                Toast.LENGTH_LONG
            ).show()
            restoreButton.isEnabled = true
        } else {
            Toast.makeText(context, "Failed to create backup", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCurrencySpinner() {
        val availableCurrencies = listOf(
            "USD" to "US Dollar",
            "EUR" to "Euro",
            "GBP" to "British Pound",
            "JPY" to "Japanese Yen",
            "CNY" to "Chinese Yuan",
            "INR" to "Indian Rupee",
            "AUD" to "Australian Dollar",
            "CAD" to "Canadian Dollar",
            "LKR" to "Sri Lankan Rupees"
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            availableCurrencies.map { "${it.first} - ${it.second}" }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        currencySpinner.adapter = adapter

        // Set current selection
        val currentCurrency = userPreferences.getCurrency()
        val position = availableCurrencies.indexOfFirst { it.first == currentCurrency }
        if (position != -1) {
            currencySpinner.setSelection(position)
        }
    }

    private fun setupPinChangeButton() {
        changePinButton.setOnClickListener {
            showChangePinDialog()
        }
    }

    private fun showChangePinDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_change_pin, null)

        val currentPinInput = dialogView.findViewById<TextInputEditText>(R.id.currentPinInput)
        val newPinInput = dialogView.findViewById<TextInputEditText>(R.id.newPinInput)
        val confirmPinInput = dialogView.findViewById<TextInputEditText>(R.id.confirmPinInput)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.change_pin)
            .setView(dialogView)
            .setPositiveButton(R.string.change) { _, _ ->
                val currentPin = currentPinInput.text?.toString() ?: ""
                val newPin = newPinInput.text?.toString() ?: ""
                val confirmPin = confirmPinInput.text?.toString() ?: ""

                when {
                    currentPin.length != 4 -> {
                        Toast.makeText(context, R.string.pin_length_error, Toast.LENGTH_SHORT).show()
                    }
                    !userPreferences.validatePin(currentPin) -> {
                        Toast.makeText(context, R.string.incorrect_pin_error, Toast.LENGTH_SHORT).show()
                    }
                    newPin.length != 4 -> {
                        Toast.makeText(context, R.string.pin_length_error, Toast.LENGTH_SHORT).show()
                    }
                    newPin != confirmPin -> {
                        Toast.makeText(context, R.string.pin_mismatch_error, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        userPreferences.setPin(newPin)
                        Toast.makeText(context, R.string.pin_change_success, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun saveCurrencySettings() {
        val selectedCurrency = currencySpinner.selectedItem.toString().split(" - ")[0]
        userPreferences.setCurrency(selectedCurrency)
        Toast.makeText(context, "Currency settings saved", Toast.LENGTH_SHORT).show()
    }
} 