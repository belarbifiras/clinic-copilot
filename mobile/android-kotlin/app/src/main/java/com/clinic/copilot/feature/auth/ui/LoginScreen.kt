package com.clinic.copilot.feature.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clinic.copilot.R
import com.clinic.copilot.ui.theme.ChatColors

/**
 * Écran de connexion – Membre 1.
 *
 * Maquette : fond F7FAFE, logo centré, champs e-mail + mot de passe,
 * "Se souvenir de moi", bouton primaire bleu, séparateur "OU CONTINUER AVEC",
 * boutons Biométrie + Google, avertissement urgence bas de page.
 *
 * @param onConnecte      Callback déclenché après connexion réussie, reçoit le userId.
 * @param onAllerInscription Navigation vers l'écran d'inscription.
 * @param viewModel       ViewModel injecté par Hilt.
 */
@Composable
fun LoginScreen(
    onConnecte: (String) -> Unit,
    onAllerInscription: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigation automatique après connexion réussie
    LaunchedEffect(uiState.utilisateurConnecteId) {
        uiState.utilisateurConnecteId?.let { onConnecte(it) }
    }

    var email by rememberSaveable { mutableStateOf("") }
    var motDePasse by rememberSaveable { mutableStateOf("") }
    var mdpVisible by rememberSaveable { mutableStateOf(false) }
    var souvenir by rememberSaveable { mutableStateOf(false) }

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

            // ── Logo + nom ──────────────────────────────────────────────────
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

            // ── Onglets Se connecter / S'inscrire ───────────────────────────
            AuthTabs(selected = 0, onLoginClick = {}, onRegisterClick = onAllerInscription)

            Spacer(Modifier.height(24.dp))

            // ── Champ e-mail ────────────────────────────────────────────────
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

            // ── Champ mot de passe ──────────────────────────────────────────
            OutlinedTextField(
                value = motDePasse,
                onValueChange = { motDePasse = it; viewModel.effacerErreur() },
                label = { Text("Mot de passe") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ChatColors.primaryBlue) },
                trailingIcon = {
                    IconButton(onClick = { mdpVisible = !mdpVisible }) {
                        Icon(
                            imageVector = if (mdpVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (mdpVisible) "Masquer" else "Afficher",
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

            // ── Se souvenir + Mot de passe oublié ──────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Checkbox(checked = souvenir, onCheckedChange = { souvenir = it })
                Text("Se souvenir de moi", fontSize = 13.sp, color = ChatColors.textSecondary)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Mot de passe oublié ?",
                    fontSize = 13.sp,
                    color = ChatColors.primaryBlue,
                    modifier = Modifier.clickable { /* TODO: reset password flow */ },
                )
            }

            Spacer(Modifier.height(4.dp))

            // ── Message d'erreur ────────────────────────────────────────────
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

            // ── Bouton Se connecter ─────────────────────────────────────────
            Button(
                onClick = { viewModel.seConnecter(email, motDePasse) },
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
                    Text("Se connecter", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Séparateur ─────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Divider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                Text("  OU CONTINUER AVEC  ", fontSize = 11.sp, color = ChatColors.textSecondary)
                Divider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
            }

            Spacer(Modifier.height(16.dp))

            // ── Boutons Biométrie + Google ──────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { /* TODO: biometrie */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                ) {
                    Text("Biométrie", color = ChatColors.textMain, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = { /* TODO: google signin */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                ) {
                    Text("Google", color = ChatColors.textMain, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(24.dp))

            // ── Avertissements bas de page ──────────────────────────────────
            AuthFooter()
        }
    }
}

/** Onglets partagés entre LoginScreen et RegisterScreen. */
@Composable
internal fun AuthTabs(selected: Int, onLoginClick: () -> Unit, onRegisterClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFFEAF0FB)),
        horizontalArrangement = Arrangement.Center,
    ) {
        listOf("Se connecter", "S'inscrire").forEachIndexed { index, label ->
            val actif = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(if (actif) ChatColors.primaryBlue else Color.Transparent)
                    .clickable { if (index == 0) onLoginClick() else onRegisterClick() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (actif) Color.White else ChatColors.textSecondary,
                    fontWeight = if (actif) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

/** Bas de page : certifications + urgence. */
@Composable
internal fun AuthFooter() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CertifBadge("Certifié HDS")
            Text("  •  ", color = ChatColors.textSecondary, fontSize = 11.sp)
            CertifBadge("Médecine vér...")
            Text("  •  ", color = ChatColors.textSecondary, fontSize = 11.sp)
            CertifBadge("Triage 24/7")
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = buildAnnotatedString {
                append("Urgence vitale immédiate ? Composez le ")
                withStyle(SpanStyle(color = ChatColors.urgenceCritique, fontWeight = FontWeight.Bold)) {
                    append("15 (SAMU)")
                }
            },
            fontSize = 12.sp,
            color = ChatColors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CertifBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFEAF5FF))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text = text, fontSize = 10.sp, color = ChatColors.primaryBlue)
    }
}

/** Couleurs communes des champs de saisie auth. */
@Composable
internal fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ChatColors.primaryBlue,
    unfocusedBorderColor = Color(0xFFE2E8F0),
    focusedLabelColor = ChatColors.primaryBlue,
    cursorColor = ChatColors.primaryBlue,
)
