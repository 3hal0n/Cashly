package com.example.cashly.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.cashly.R
import com.example.cashly.data.IntroSlide
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout

class SplashActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var getStartedButton: MaterialButton
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        viewPager = findViewById(R.id.viewPager)
        getStartedButton = findViewById(R.id.getStartedButton)
        tabLayout = findViewById(R.id.tabLayout)

        val introSlides = listOf(
            IntroSlide(
                R.drawable.expenses,
                "Track Expenses",
                "Keep track of your daily expenses and income with ease"
            ),
            IntroSlide(
                R.drawable.budgets,
                "Set Budgets",
                "Set monthly budgets and get alerts when you're close to limits"
            ),
            IntroSlide(
                R.drawable.analytics,
                "View Analytics",
                "Analyze your spending patterns with detailed charts and reports"
            )
        )

        viewPager.adapter = IntroSlideAdapter(introSlides)
        
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        getStartedButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
} 