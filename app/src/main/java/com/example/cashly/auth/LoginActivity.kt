package com.example.cashly.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.cashly.MainActivity
import com.example.cashly.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {
    private lateinit var pinEditText: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var biometricLoginButton: MaterialButton
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        pinEditText = findViewById(R.id.pinEditText)
        loginButton = findViewById(R.id.loginButton)
        biometricLoginButton = findViewById(R.id.biometricLoginButton)

        setupBiometricLogin()
        setupPinLogin()
    }

    private fun setupPinLogin() {
        loginButton.setOnClickListener {
            val pin = pinEditText.text.toString()
            if (validatePin(pin)) {
                startMainActivity()
            } else {
                Toast.makeText(this, "Invalid PIN", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBiometricLogin() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    startMainActivity()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@LoginActivity, errString, Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Login to Cashly")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Cancel")
            .build()

        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                biometricLoginButton.isEnabled = true
                biometricLoginButton.setOnClickListener { 
                    biometricPrompt.authenticate(promptInfo)
                }
            }
            else -> {
                biometricLoginButton.isEnabled = false
            }
        }
    }

    private fun validatePin(pin: String): Boolean {
        return pin == "1234"
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
} 