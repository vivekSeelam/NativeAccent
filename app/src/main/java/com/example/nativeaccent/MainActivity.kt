package com.example.nativeaccent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.nativeaccent.ui.NativeAccentApp
import com.example.nativeaccent.ui.theme.NativeAccentTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            NativeAccentTheme {
                NativeAccentApp(onExit = { finish() })
            }
        }
    }
}
