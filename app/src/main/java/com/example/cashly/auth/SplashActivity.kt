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
    private val handler = Handler(Looper.getMainLooper())
    private val autoSlideRunnable = object : Runnable {
        override fun run() {
            val nextItem = if (viewPager.currentItem + 1 >= viewPager.adapter?.itemCount ?: 0) {
                0
            } else {
                viewPager.currentItem + 1
            }
            viewPager.setCurrentItem(nextItem, true)
            handler.postDelayed(this, 3000) // Slide every 3 seconds
        }
    }

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

        // Start auto-sliding
        handler.postDelayed(autoSlideRunnable, 3000)

        // Pause auto-sliding when user interacts with the ViewPager
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                handler.removeCallbacks(autoSlideRunnable)
                handler.postDelayed(autoSlideRunnable, 3000)
            }
        })

        getStartedButton.setOnClickListener {
            handler.removeCallbacks(autoSlideRunnable)
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(autoSlideRunnable)
    }
} 