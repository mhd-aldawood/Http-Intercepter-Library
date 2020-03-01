package com.chuckerteam.chucker.internal.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.chuckerteam.chucker.R
import com.chuckerteam.chucker.api.Chucker
import com.chuckerteam.chucker.internal.ui.throwable.ThrowableActivity
import com.chuckerteam.chucker.internal.ui.throwable.ThrowableAdapter
import com.chuckerteam.chucker.internal.ui.transaction.TransactionActivity
import com.chuckerteam.chucker.internal.ui.transaction.TransactionAdapter
import com.google.android.material.tabs.TabLayout

internal class MainActivity :
    BaseChuckerActivity(),
    TransactionAdapter.TransactionClickListListener,
    ThrowableAdapter.ThrowableClickListListener {

    private lateinit var viewModel: MainViewModel
    private lateinit var viewPager: ViewPager

    private val applicationName: CharSequence
        get() = applicationInfo.loadLabel(packageManager)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chucker_activity_main)

        val toolbar = findViewById<Toolbar>(R.id.chuckerTransactionToolbar)
        setSupportActionBar(toolbar)
        toolbar.subtitle = applicationName

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        viewPager = findViewById(R.id.chuckerMainViewPager)
        viewPager.adapter = HomePageAdapter(this, supportFragmentManager)

        val tabLayout = findViewById<TabLayout>(R.id.chuckerMainTabLayout)
        tabLayout.setupWithViewPager(viewPager)

        viewPager.addOnPageChangeListener(object : TabLayout.TabLayoutOnPageChangeListener(tabLayout) {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == 0) {
                    Chucker.dismissTransactionsNotification(this@MainActivity)
                } else {
                    Chucker.dismissErrorsNotification(this@MainActivity)
                }
            }
        })
        consumeIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeIntent(intent)
    }

    /**
     * Scroll to the right tab.
     */
    private fun consumeIntent(intent: Intent) {
        // Get the screen to show, by default => HTTP
        val screenToShow = intent.getIntExtra(EXTRA_SCREEN, Chucker.SCREEN_HTTP)
        if (screenToShow == Chucker.SCREEN_HTTP) {
            viewPager.currentItem = HomePageAdapter.SCREEN_HTTP_INDEX
        } else {
            viewPager.currentItem = HomePageAdapter.SCREEN_THROWABLE_INDEX
        }
    }

    override fun onThrowableClick(throwableId: Long, position: Int) {
        ThrowableActivity.start(this, throwableId)
    }

    override fun onTransactionClick(transactionId: Long, position: Int) {
        TransactionActivity.start(this, transactionId)
    }

    companion object {
        const val EXTRA_SCREEN = "EXTRA_SCREEN"
    }
}
