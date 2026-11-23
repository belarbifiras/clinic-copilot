package com.clinic.copilot.feature.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clinic.copilot.R
import com.clinic.copilot.ui.theme.ChatColors

/**
 * Écran d'inscription – Membre 1.
 *
 * Champs : Prénom, Nom, E-mail, Mot de passe (avec œil), bouton primaire,
 * onglets partagés avec LoginScreen, même footer.
 *
 * @param onInscrit       Callback après inscription réussie, reçoit le userId.
 * @param onAllerConnexion Navigation retour vers LoginScreen.
 * @param viewModel       ViewModel injecté par Hilt.
 */
@Composable
fun RegisterScreen(
    onInscrit: (String) -> Unit,
    onAllerConnexion: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.utilisateurConnecteId) {
        uiState.utilisateurConnecteId?.let { onInscrit(it) }
    }

    var prenom by rememberSaveable { mutableStateOf("") }
    var nom by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var motDePasse by rememberSaveable { mutableStateOf("") }
    var mdpVisible by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatColors.medicalBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Logo ────────────────────────────────────────────────────────
            Image(
                painter = painterResource(id = R.drawable.ic_copilot_mascot),
                contentDescription = "Clinic Copilot",
                modifier = Modifier.size(80.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Clinic Copilot",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = ChatColors.primaryNavy,
            )
            Text(
                text = "Votre santé, guidée intelligemment",
                fontSize = 13.sp,
                color = ChatColors.textSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))

            // ── Onglets ─────────────────────────────────────────────────────
            AuthTabs(selected = 1, onLoginClick = onAllerConnexion, onRegisterClick = {})

            Spacer(Modifier.height(24.dp))

            // ── Prénom ──────────────────────────────────────────────────────
            OutlinedTextField(
                value = prenom,
                onValueChange = { prenom = it; viewModel.effacerErreur() },
                label = { Text("Prénom") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ChatColors.primaryBlue) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = authFieldColors(),
            )

            Spacer(Modifier.height(12.dp))

            // ── Nom ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value = nom,
                onValueChange = { nom = it; viewModel.effacerErreur() },
                label = { Text("Nom") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ChatColors.primaryBlue) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = authFieldColors(),
            )

            Spacer(Modifier.height(12.dp))

            // ── E-mail ──────────────────────────────────────────────────────
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; viewModel.effacerErreur() },
                label = { Text("Adresse e-mail") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = ChatColors.primaryBlue) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = authFieldColors(),
            )

            Spacer(Modifier.height(12.dp))

            // ── Mot de passe ────────────────────────────────────────────────
            OutlinedTextField(
                value = motDePasse,
                onValueChange = { motDePasse = it; viewModel.effacerErreur() },
                label = { Text("Mot de passe (min. 6 caractères)") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ChatColors.primaryBlue) },
                trailingIcon = {
                    IconButton(onClick = { mdpVisible = !mdpVisible }) {
                        Icon(
                            imageVector = if (mdpVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = ChatColors.textSecondary,
                        )
                    }
                },
                visualTransformation = if (mdpVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = authFieldColors(),
            )

            Spacer(Modifier.height(8.dp))

            // ── Erreur ──────────────────────────────────────────────────────
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = ChatColors.urgenceCritique,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Bouton S'inscrire ───────────────────────────────────────────
            Button(
                onClick = { viewModel.sInscrire(prenom, nom, email, motDePasse) },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChatColors.primaryBlue,
                    contentColor = Color.White,
                ),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("S'inscrire", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(24.dp))

            AuthFooter()
        }
    }
}
