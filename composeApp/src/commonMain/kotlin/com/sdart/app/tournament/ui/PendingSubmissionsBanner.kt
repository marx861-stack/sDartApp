package com.sdart.app.tournament.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdart.app.tournament.TournamentManager
import com.sdart.app.ui.theme.PrimaryOrange

@Composable
fun PendingSubmissionsBanner(
    tournamentManager: TournamentManager,
    modifier: Modifier = Modifier
) {
    val submissions = tournamentManager.pendingSubmissions
    val currentSub = submissions.firstOrNull()

    AnimatedVisibility(
        visible = currentSub != null,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        if (currentSub != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF2C2417),
                border = BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Meldung aus Web",
                                color = PrimaryOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (submissions.size > 1) {
                                Text(
                                    text = " (+${submissions.size - 1} weitere)",
                                    color = Color(0x99FFFFFF),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${currentSub.player1Name} ${currentSub.submission.player1Legs} : ${currentSub.submission.player2Legs} ${currentSub.player2Name}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Sieger: ${currentSub.winnerName}",
                            color = Color(0xCCFFFFFF),
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { tournamentManager.rejectSubmission(currentSub.id) },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF453A)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Ablehnen", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { tournamentManager.approveSubmission(currentSub.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Übernehmen", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
