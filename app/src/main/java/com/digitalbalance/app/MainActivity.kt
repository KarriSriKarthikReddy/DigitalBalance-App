package com.digitalbalance.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.digitalbalance.app.data.usage.UsageStatsDataSource
import com.digitalbalance.app.ui.DigitalBalanceApp
import com.digitalbalance.app.ui.theme.DigitalBalanceTheme
import com.digitalbalance.app.ui.usage.UsageViewModel

class MainActivity : ComponentActivity() {
    private val usageViewModel: UsageViewModel by lazy {
        ViewModelProvider(
            this,
            UsageViewModel.factory(UsageStatsDataSource(applicationContext))
        )[UsageViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DigitalBalanceTheme {
                val state by usageViewModel.uiState.collectAsState()
                DigitalBalanceApp(
                    usageState = state,
                    onOpenUsageSettings = ::openUsageAccessSettings,
                    onRefreshUsage = usageViewModel::refresh
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        usageViewModel.refresh()
    }

    private fun openUsageAccessSettings() {
        try {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}
