package com.clinic.copilot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.clinic.copilot.navigation.ChatNavigation
import com.clinic.copilot.ui.theme.ClinicCopilotTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClinicCopilotTheme {
                // ConversationListScreen est le point d'entrée initial de l'application pour le moment,
                // car la feature de messagerie (ChatNavigation) est la première brique fonctionnelle développée de Clinic Copilot.
                ChatNavigation(
                    currentUserId = "patient-demo",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// Les fonctions Greeting() et GreetingPreview() du template par défaut ont été supprimées
// car elles ne sont plus utilisées comme point d'entrée de l'application.
