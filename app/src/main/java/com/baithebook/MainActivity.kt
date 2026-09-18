package com.baithebook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.baithebook.garcia.services.MainViewModel
import com.baithebook.garcia.ui.foundation.AppShell

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel = remember { MainViewModel() }
            AppShell(viewModel = viewModel)
        }
    }
}
