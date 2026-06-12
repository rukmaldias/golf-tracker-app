package com.rapsodo.golf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.rapsodo.golf.ui.theme.RapsodogolftrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import io.github.aakira.napier.Napier

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RapsodogolftrackerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { }
            }
        }

        //Napier.v("Hello napier")
        //Napier.d("optional tag", tag = "your tag")
    }
}