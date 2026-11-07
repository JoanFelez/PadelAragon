package com.padelaragon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.padelaragon.app.ui.navigation.NavGraph
import com.padelaragon.app.ui.theme.PadelAragonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PadelAragonTheme {
                NavGraph()
            }
        }
    }
}
