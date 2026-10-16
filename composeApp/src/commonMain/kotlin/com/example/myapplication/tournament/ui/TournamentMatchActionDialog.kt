package com.example.myapplication.tournament.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.PrimaryOrange
import com.example.myapplication.tournament.TournamentGameConfig
import com.example.myapplication.tournament.TournamentMatch

/**
 * Apple-Sports Dialog zur Spielauswahl und manuellen Ergebniserfassung für den Host.
 * Ermöglicht sowohl das Starten des Spiels in der App als auch die präzise Eingabe
 * der gewonnenen Legs beider Spieler.
 */
@Composable
fun TournamentMatchActionDialog(
    match: TournamentMatch,
    tournamentConfig: TournamentGameConfig,
    onStartMatch: (TournamentMatch) -> Unit,
    onSubmitResult: (matchId: String, winnerId: String, p1Legs: Int, p2Legs: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val p1 = match.player1
    val p2 = match.player2
    val maxLegs = (tournamentConfig.legs.takeIf { it > 0 } ?: 3)

    var p1Legs by remember { mutableStateOf(match.player1Result.legsWon) }
    var p2Legs by remember { mutableStateOf(match.player2Result.legsWon) }

    val hasValidWinner = (p1Legs != p2Legs) && (p1Legs > 0 || p2Legs > 0)
    val winnerName = when {
        p1Legs > p2Legs -> p1?.name ?: "Spieler 1"
        p2Legs > p1Legs -> p2?.name ?: "Spieler 2"
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(22.dp),
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (match.isFinished) "Ergebnis bearbeiten" else "Match Runde ${match.round}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${tournamentConfig.mode.name} • Best of $maxLegs Legs",
                    color = Color(0x99FFFFFF),
                    fontSize = 12.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Spieler-Vergleichskarte (Apple Inset Style)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x14FFFFFF),
                    border = BorderStroke(0.5.dp, Color(0x1FFFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Spieler 1
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0x26FF8800),
                                border = BorderStroke(0.5.dp, PrimaryOrange.copy(alpha = 0.5f)),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = p1?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "1",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = p1?.name ?: "Spieler 1",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // VS Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x1FFFFFFF)
                        ) {
                            Text(
                                text = "VS",
                                color = Color(0x80FFFFFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        // Spieler 2
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0x1FFFFFFF),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = p2?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "2",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = p2?.name ?: "Spieler 2",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Action Button (nur wenn noch nicht fertig gespielt)
                if (!match.isFinished) {
                    Button(
                        onClick = { onStartMatch(match) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = "🎯 In App spielen",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Feine Trennlinie
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(0.5.dp)
                                .background(Color(0x22FFFFFF))
                        )
                        Text(
                            text = " ODER MANUELL ERFASSEN ",
                            color = Color(0x66FFFFFF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(0.5.dp)
                                .background(Color(0x22FFFFFF))
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Leg Stepper Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x0EFFFFFF),
                    border = BorderStroke(0.5.dp, Color(0x1AFFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Gewonnene Legs",
                            color = Color(0x80FFFFFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Stepper Spieler 1
                        LegStepperRow(
                            playerName = p1?.name ?: "Spieler 1",
                            legs = p1Legs,
                            maxLegs = maxLegs,
                            onLegsChanged = { p1Legs = it }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Stepper Spieler 2
                        LegStepperRow(
                            playerName = p2?.name ?: "Spieler 2",
                            legs = p2Legs,
                            maxLegs = maxLegs,
                            onLegsChanged = { p2Legs = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Gewinner-Vorschau oder Warnhinweis
                if (hasValidWinner && winnerName != null) {
                    Text(
                        text = "🏆 Sieger: $winnerName ($p1Legs : $p2Legs)",
                        color = PrimaryOrange,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                } else if (p1Legs > 0 && p1Legs == p2Legs) {
                    Text(
                        text = "Unentschieden im Darts nicht möglich",
                        color = Color(0xFFFF453A),
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (hasValidWinner && p1 != null && p2 != null) {
                        val winnerId = if (p1Legs > p2Legs) p1.id else p2.id
                        onSubmitResult(match.id, winnerId, p1Legs, p2Legs)
                    }
                },
                enabled = hasValidWinner && p1 != null && p2 != null,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryOrange,
                    disabledContainerColor = Color(0x1AFFFFFF),
                    disabledContentColor = Color(0x4DFFFFFF)
                )
            ) {
                Text(
                    text = if (match.isFinished) "Ergebnis aktualisieren" else "Ergebnis erfassen",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen", color = Color(0x80FFFFFF), fontSize = 13.sp)
            }
        }
    )
}

@Composable
private fun LegStepperRow(
    playerName: String,
    legs: Int,
    maxLegs: Int,
    onLegsChanged: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x0DFFFFFF), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = playerName,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Minus Button
            Surface(
                shape = CircleShape,
                color = if (legs > 0) Color(0x22FFFFFF) else Color(0x08FFFFFF),
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(enabled = legs > 0) {
                        if (legs > 0) onLegsChanged(legs - 1)
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "−",
                        color = if (legs > 0) Color.White else Color(0x33FFFFFF),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Score Anzeige
            Text(
                text = legs.toString(),
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(26.dp)
            )

            // Plus Button
            Surface(
                shape = CircleShape,
                color = if (legs < maxLegs) Color(0x22FFFFFF) else Color(0x08FFFFFF),
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(enabled = legs < maxLegs) {
                        if (legs < maxLegs) onLegsChanged(legs + 1)
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "+",
                        color = if (legs < maxLegs) Color.White else Color(0x33FFFFFF),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
