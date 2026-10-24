package com.sdart.app.tournament.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdart.app.tournament.LeagueStandingRow
import com.sdart.app.tournament.TournamentMatch
import com.sdart.app.tournament.TournamentState
import com.sdart.app.ui.theme.PrimaryOrange

@Composable
fun TournamentLeagueView(
    state: TournamentState,
    standings: List<LeagueStandingRow>,
    onMatchClick: (TournamentMatch) -> Unit,
    modifier: Modifier = Modifier,
    // Gruppenphase: so viele Plätze oben kommen in die K.-o.-Runde
    qualifierCount: Int = 0
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 960.dp)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // 1. APPLE INSET-GROUPED LIGA-TABELLE
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0x0CFFFFFF),
                border = BorderStroke(0.5.dp, Color(0x1FFFFFFF))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tabelle",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        )

                        Text(
                            text = if (qualifierCount > 0) {
                                "${standings.size} Teilnehmer · $qualifierCount kommen weiter"
                            } else {
                                "${standings.size} Teilnehmer"
                            },
                            color = Color(0x66FFFFFF),
                            fontSize = 11.sp
                        )
                    }

                    // Tabellenkopf
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#", Modifier.weight(0.08f), color = Color(0x66FFFFFF), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text("Spieler", Modifier.weight(0.38f), color = Color(0x66FFFFFF), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text("Sp", Modifier.weight(0.11f), color = Color(0x66FFFFFF), fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                        Text("S", Modifier.weight(0.11f), color = Color(0x66FFFFFF), fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                        Text("N", Modifier.weight(0.11f), color = Color(0x66FFFFFF), fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                        Text("Diff", Modifier.weight(0.11f), color = Color(0x66FFFFFF), fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                        Text("Ø", Modifier.weight(0.10f), color = Color(0x66FFFFFF), fontSize = 11.sp, textAlign = TextAlign.End, fontWeight = FontWeight.Medium)
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = Color(0x14FFFFFF))

                    // Tabellenzeilen
                    standings.forEachIndexed { idx, row ->
                        val isLeader = row.rank == 1 && row.won > 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rang-Badge
                            Box(
                                modifier = Modifier.weight(0.08f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                when (row.rank) {
                                    1 -> {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0x2BFF8800),
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("1", color = PrimaryOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    2 -> {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0x1FFFFFFF),
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("2", color = Color(0xCCFFFFFF), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                    3 -> {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0x18CD7F32),
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("3", color = Color(0xFFCD7F32), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                    else -> {
                                        Text(
                                            text = "${row.rank}.",
                                            color = Color(0x66FFFFFF),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            // Spielername mit Initiale
                            Row(
                                modifier = Modifier.weight(0.38f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (row.rank <= qualifierCount) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color(0xFF4CAF50), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = row.player.name,
                                    color = if (isLeader) Color.White else Color(0xCCFFFFFF),
                                    fontSize = 13.sp,
                                    fontWeight = if (isLeader) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Spiele
                            Text(
                                text = row.played.toString(),
                                modifier = Modifier.weight(0.11f),
                                color = Color(0x99FFFFFF),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )

                            // Siege
                            Text(
                                text = row.won.toString(),
                                modifier = Modifier.weight(0.11f),
                                color = if (row.won > 0) Color.White else Color(0x66FFFFFF),
                                fontSize = 12.sp,
                                fontWeight = if (row.won > 0) FontWeight.SemiBold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )

                            // Niederlagen
                            Text(
                                text = row.lost.toString(),
                                modifier = Modifier.weight(0.11f),
                                color = Color(0x66FFFFFF),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )

                            // Differenz-Kapsel
                            Box(
                                modifier = Modifier.weight(0.11f),
                                contentAlignment = Alignment.Center
                            ) {
                                val diff = row.legDifference
                                val diffText = if (diff > 0) "+$diff" else "$diff"
                                when {
                                    diff > 0 -> {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0x1C4CAF50)
                                        ) {
                                            Text(
                                                text = diffText,
                                                color = Color(0xFF81C784),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    diff < 0 -> {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0x1CE53935)
                                        ) {
                                            Text(
                                                text = diffText,
                                                color = Color(0xFFE57373),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    else -> {
                                        Text("0", color = Color(0x66FFFFFF), fontSize = 11.sp)
                                    }
                                }
                            }

                            // 3-Dart Average
                            Text(
                                text = if (row.averageScore > 0) row.averageScore.toString() else "—",
                                modifier = Modifier.weight(0.10f),
                                color = Color(0x80FFFFFF),
                                fontSize = 11.sp,
                                textAlign = TextAlign.End
                            )
                        }

                        if (idx < standings.size - 1) {
                            HorizontalDivider(thickness = 0.5.dp, color = Color(0x0AFFFFFF))
                        }
                    }
                }
            }
        }

        // 2. BEGEGNUNGEN NACH RUNDEN (Gleicher Apple Sports Match Card Look)
        item {
            Text(
                text = "Begegnungen",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }

        val groupedMatches = state.matches.groupBy { it.round }.toSortedMap()

        groupedMatches.forEach { (round, matches) ->
            item {
                val doneCount = matches.count { it.isFinished }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Runde $round",
                        color = Color(0x80FFFFFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$doneCount/${matches.size} beendet",
                        color = Color(0x4DFFFFFF),
                        fontSize = 11.sp
                    )
                }
            }

            items(matches) { match ->
                BracketMatchCard(
                    match = match,
                    onClick = {
                        if (match.isReady || (match.isFinished && match.player1 != null && match.player2 != null)) {
                            onMatchClick(match)
                        }
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
}
