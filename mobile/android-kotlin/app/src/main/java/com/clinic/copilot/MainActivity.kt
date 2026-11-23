package com.clinic.copilot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.clinic.copilot.navigation.AppNavGraph
import com.clinic.copilot.ui.theme.ClinicCopilotTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClinicCopilotTheme(dynamicColor = false) {
                // AppNavGraph orchestre l'ensemble du parcours :
                // Auth (Membre 1) → Home + Triage (Membre 2) → Messagerie (Membre 3)
                AppNavGraph(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
