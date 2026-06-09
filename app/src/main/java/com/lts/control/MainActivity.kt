package com.lts.control

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.lifecycle.ViewModelProvider
import com.lts.control.core.ble.BleViewModel
import com.lts.control.ui.LtsControlApp
import com.example.ltscontrol.ui.theme.LTSControlTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val vm = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[BleViewModel::class.java]

        setContent {
            LTSControlTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding() // 老 Compose 稳定支持
                ) {
                    LtsControlApp(vm)
                }
            }
        }
    }
}
