package com.clinic.copilot.feature.notifications.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.clinic.copilot.core.data.LocalNotification
import com.clinic.copilot.core.data.NotificationType
import com.clinic.copilot.ui.theme.ChatColors

/**
 * Centre de notifications in-app (Sprint 7, US-60 & US-61).
 */
@Composable
fun NotificationsDialog(
    notifications: List<LocalNotification>,
    onDismiss: () -> Unit,
    onMarquerLue: (String) -> Unit = {},
    onToutMarquerLu: () -> Unit = {},
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ChatColors.cardSurface,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ChatColors.primaryBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = ChatColors.primaryBlue,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Notifications",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChatColors.primaryNavy,
                            )
                            val nonLues = notifications.count { !it.estLue }
                            Text(
                                if (nonLues > 0) "$nonLues non lue(s)" else "Toutes lues",
                                fontSize = 12.sp,
                                color = if (nonLues > 0) ChatColors.primaryBlue else ChatColors.textSecondary,
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = ChatColors.textSecondary)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Tout marquer comme lu
                if (notifications.any { !it.estLue }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onToutMarquerLu)
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, tint = ChatColors.primaryBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Tout marquer comme lu", fontSize = 12.sp, color = ChatColors.primaryBlue, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Liste
                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Aucune notification pour le moment", color = ChatColors.textSecondary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(notifications) { notif ->
                            NotificationCard(
                                notif = notif,
                                onClick = { onMarquerLue(notif.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(notif: LocalNotification, onClick: () -> Unit) {
    val bg = if (notif.estLue) Color(0xFFF8FAFC) else Color(0xFFEAF5FF)
    val border = if (notif.estLue) Color(0xFFE2E8F0) else ChatColors.primaryBlue.copy(alpha = 0.3f)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            val icon = when (notif.type) {
                NotificationType.RAPPEL_RDV -> Icons.Default.CalendarToday
                NotificationType.PILULIER -> Icons.Default.Medication
                NotificationType.TRIAGE -> Icons.Default.SmartToy
                NotificationType.MESSAGE -> Icons.Default.Notifications
            }
            val iconColor = when (notif.type) {
                NotificationType.RAPPEL_RDV -> ChatColors.primaryBlue
                NotificationType.PILULIER -> Color(0xFF10B981)
                NotificationType.TRIAGE -> ChatColors.turquoiseAccent
                NotificationType.MESSAGE -> Color(0xFFF59E0B)
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(17.dp))
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = notif.titre,
                        fontWeight = if (!notif.estLue) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = ChatColors.primaryNavy,
                    )
                    Text(
                        text = notif.dateTexte,
                        fontSize = 10.sp,
                        color = ChatColors.textSecondary,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = notif.message,
                    fontSize = 12.sp,
                    color = ChatColors.textMain,
                    lineHeight = 16.sp,
                )
            }

            if (!notif.estLue) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ChatColors.primaryBlue),
                )
            }
        }
    }
}
