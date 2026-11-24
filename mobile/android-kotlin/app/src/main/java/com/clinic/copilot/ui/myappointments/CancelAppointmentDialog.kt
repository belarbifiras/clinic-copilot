package com.clinic.copilot.ui.myappointments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.clinic.copilot.domain.Appointment
import com.clinic.copilot.ui.theme.AppointmentColors
import com.clinic.copilot.ui.theme.AppointmentFormatters
import com.clinic.copilot.ui.theme.ChatTypography

/**
 * Dialog de confirmation d'annulation d'un rendez-vous (US-33), au design Stitch.
 *
 * Le texte rappelle le médecin et la date afin d'éviter une annulation par erreur.
 * Toute la décision métier reste dans le ViewModel : ce composable ne fait que
 * transmettre les intentions de l'utilisateur.
 *
 * @param rendezVous Rendez-vous concerné par l'annulation.
 * @param operationEnCours `true` pendant l'appel réseau ; le bouton est désactivé.
 * @param onConfirmer Callback déclenchant l'annulation côté ViewModel.
 * @param onAnnuler Callback fermant le dialog sans modifier la liste.
 */
@Composable
fun CancelAppointmentDialog(
    rendezVous: Appointment,
    operationEnCours: Boolean,
    onConfirmer: () -> Unit,
    onAnnuler: () -> Unit,
) {
    val dateComplete = AppointmentFormatters.formaterDateComplete(rendezVous.date)
    val heure = AppointmentFormatters.formaterHeure(rendezVous.heureDebut, rendezVous.heureFin)
        .substringBefore(" - ")

    AlertDialog(
        onDismissRequest = onAnnuler,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AppointmentColors.errorContainer40),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(26.dp),
                )
            }
        },
        title = {
            Text(
                text = "Annuler ce rendez-vous ?",
                style = ChatTypography.headlineSm,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Êtes-vous sûr de vouloir annuler ce rendez-vous avec " +
                        "${rendezVous.doctorName} ?",
                    style = ChatTypography.bodyMd,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppointmentColors.surfaceContainerLow)
                        .padding(12.dp),
                ) {
                    Text(
                        text = "$dateComplete à $heure",
                        style = ChatTypography.labelLg,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onConfirmer,
                    enabled = !operationEnCours,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    if (operationEnCours) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.size(8.dp))
                    }
                    Text(
                        text = "Oui, annuler définitivement",
                        style = ChatTypography.labelLg,
                    )
                }
                Button(
                    onClick = onAnnuler,
                    enabled = !operationEnCours,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppointmentColors.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Text(
                        text = "Conserver le rendez-vous",
                        style = ChatTypography.labelLg,
                    )
                }
            }
        },
        dismissButton = null,
        containerColor = AppointmentColors.surfaceContainerLowest,
        titleContentColor = MaterialTheme.colorScheme.primary,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
