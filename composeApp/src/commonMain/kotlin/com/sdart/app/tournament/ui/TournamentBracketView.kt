package com.sdart.app.tournament.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdart.app.tournament.TournamentMatch
import com.sdart.app.tournament.TournamentPlayer
import com.sdart.app.tournament.TournamentState
import com.sdart.app.ui.theme.PrimaryOrange
import org.jetbrains.compose.resources.painterResource
import sdartapp.composeapp.generated.resources.*

@Composable
fun TournamentBracketView(
    state: TournamentState,
    isWidescreen: Boolean,
    onMatchClick: (TournamentMatch) -> Unit,
    modifier: Modifier = Modifier
) {
    val rounds = remember(state.matches) {
        state.matches.groupBy { it.round }.toSortedMap()
    }
    val maxRound = rounds.keys.maxOrNull() ?: 1

    var selectedMobileRound by remember { mutableStateOf(1) }
    var mobileViewMode by remember { mutableStateOf(0) } // 0 = Rundenliste, 1 = Turnierbaum

    fun getRoundTitle(round: Int): String {
        return when {
            round == maxRound -> "Finale"
            round == maxRound - 1 -> "Halbfinale"
            round == maxRound - 2 -> "Viertelfinale"
            else -> "Runde $round"
        }
    }

    if (isWidescreen || mobileViewMode == 1) {
        // --- HORIZONTALER TURNIERBAUM MIT MATHEMATISCH EXAKTEN VERBINDUNGSLINIEN ---
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val viewportWidth = maxWidth
            val viewportHeight = maxHeight

            val horizontalScrollState = rememberScrollState()
            val verticalScrollState = rememberScrollState()

            val roundEntries = remember(rounds) { rounds.entries.toList() }
            val r1MatchCount = roundEntries.firstOrNull()?.value?.size ?: 1
            val roundCount = roundEntries.size
            val hasChampion = state.winner != null

            // DYNAMISCHE SKALIERUNG FÜR DESKTOP / WIDESCREEN
            // Ermittelt Zielbreite und -höhe dynamisch aus dem Viewport, damit der Baum auch bei 4 oder 8 Spielern
            // den Bildschirm großzügig ausfüllt und kein riesiger Leerraum entsteht.
            val availableHeight = (viewportHeight - 110.dp).coerceAtLeast(360.dp)

            val baseSlotHeight = when {
                !isWidescreen -> 112.dp
                r1MatchCount <= 2 -> (availableHeight * 0.78f / 2f).coerceIn(180.dp, 360.dp)
                r1MatchCount <= 4 -> (availableHeight * 0.84f / 4f).coerceIn(160.dp, 260.dp)
                else -> (availableHeight * 0.88f / r1MatchCount).coerceIn(112.dp, 160.dp)
            }

            val cardWidth = when {
                !isWidescreen -> 230.dp
                else -> {
                    val targetFactor = if (roundCount <= 3) 0.84f else 0.90f
                    val targetWidth = (viewportWidth * targetFactor).coerceIn(840.dp, 1750.dp)
                    val connectorFactor = 0.20f
                    val totalWeight = roundCount + (if (hasChampion) 0.85f else 0f) + (roundCount - 1 + (if (hasChampion) 1 else 0)) * connectorFactor
                    (targetWidth / totalWeight).coerceIn(250.dp, 440.dp)
                }
            }

            val connectorWidth = when {
                !isWidescreen -> 36.dp
                else -> (cardWidth * 0.20f).coerceIn(44.dp, 92.dp)
            }

            val championWidth = when {
                !isWidescreen -> 210.dp
                else -> (cardWidth * 0.85f).coerceIn(220.dp, 380.dp)
            }

            val isLargeCards = cardWidth >= 270.dp
            val isExtraLargeCards = cardWidth >= 350.dp
            val totalTreeHeight = baseSlotHeight * r1MatchCount

            Column(modifier = Modifier.fillMaxSize()) {
                if (!isWidescreen) {
                    // Mobile Switcher zurück zur Rundenansicht
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Turnierbaum (horizontal wischen)",
                            color = Color(0x80FFFFFF),
                            fontSize = 12.sp
                        )
                        TextButton(
                            onClick = { mobileViewMode = 0 },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Zu Runden-Tabs", color = PrimaryOrange, fontSize = 12.sp)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(horizontalScrollState)
                        .verticalScroll(verticalScrollState)
                ) {
                    // sizeIn(minWidth = viewportWidth, minHeight = viewportHeight) zentriert den Baum perfekt,
                    // falls das Fenster größer als der Baum ist (beseitigt Leerraum & Eck-Ausrichtung)
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = viewportWidth, minHeight = viewportHeight)
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // 1. RUNDEN-TITEL ZEILE (separat vom Raster, damit keine vertikale Verzerrung entsteht)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = if (isExtraLargeCards) 16.dp else 12.dp)
                            ) {
                                roundEntries.forEachIndexed { index, entry ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0x14FFFFFF),
                                        border = BorderStroke(0.5.dp, Color(0x1AFFFFFF)),
                                        modifier = Modifier.width(cardWidth)
                                    ) {
                                        Text(
                                            text = getRoundTitle(entry.key),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = if (isExtraLargeCards) 16.sp else if (isLargeCards) 14.sp else 12.sp
                                            ),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = if (isExtraLargeCards) 7.dp else 5.dp)
                                        )
                                    }
                                    if (index < roundEntries.size - 1) {
                                        Spacer(modifier = Modifier.width(connectorWidth))
                                    }
                                }

                                if (state.winner != null) {
                                    Spacer(modifier = Modifier.width(connectorWidth))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0x24FF8800),
                                        border = BorderStroke(0.5.dp, PrimaryOrange.copy(alpha = 0.5f)),
                                        modifier = Modifier.width(championWidth)
                                    ) {
                                        Text(
                                            text = "Sieger",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                color = PrimaryOrange,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = if (isExtraLargeCards) 16.sp else if (isLargeCards) 14.sp else 12.sp
                                            ),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = if (isExtraLargeCards) 7.dp else 5.dp)
                                        )
                                    }
                                }
                            }

                            // 2. TURNIER-RASTER MIT EXAKTER BINÄRBAUM-ZENTRIERUNG
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.height(totalTreeHeight)
                            ) {
                                roundEntries.forEachIndexed { roundIndex, entry ->
                                    val matchesInRound = entry.value
                                    val slotMultiplier = 1 shl roundIndex // 2^roundIndex: 1, 2, 4, 8...
                                    val roundBoxHeight = baseSlotHeight * slotMultiplier

                                    // Spalte mit Match-Karten für diese Runde
                                    Column(
                                        modifier = Modifier
                                            .width(cardWidth)
                                            .height(totalTreeHeight)
                                    ) {
                                        matchesInRound.forEach { match ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(roundBoxHeight),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                BracketMatchCard(
                                                    match = match,
                                                    onClick = {
                                                        if (match.isReady || (match.isFinished && match.player1 != null && match.player2 != null)) {
                                                            onMatchClick(match)
                                                        }
                                                    },
                                                    isLarge = isLargeCards,
                                                    isExtraLarge = isExtraLargeCards,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }

                                    // 1dp Verbindungslinien zur nächsten Runde
                                    val nextRoundMatches = roundEntries.getOrNull(roundIndex + 1)?.value
                                    if (nextRoundMatches != null) {
                                        BracketConnectorCanvas(
                                            leftRoundIndex = roundIndex,
                                            leftMatches = matchesInRound,
                                            slotHeightR1 = baseSlotHeight,
                                            modifier = Modifier
                                                .width(connectorWidth)
                                                .height(totalTreeHeight)
                                        )
                                    }
                                }

                                // Verbindung & Kachel für den Champion
                                state.winner?.let { champion ->
                                    FinalToChampionConnector(
                                        finalY = totalTreeHeight / 2,
                                        hasChampion = true,
                                        modifier = Modifier
                                            .width(connectorWidth)
                                            .height(totalTreeHeight)
                                    )

                                    Column(
                                        modifier = Modifier
                                            .width(championWidth)
                                            .height(totalTreeHeight)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(totalTreeHeight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            ChampionPodiumCard(
                                                champion = champion,
                                                isLarge = isLargeCards,
                                                isExtraLarge = isExtraLargeCards
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // --- MOBILE / SMARTPHONE: CUPERTINO SEGMENTED TABS ---
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Segmented Control für Runden
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x12FFFFFF),
                border = BorderStroke(0.5.dp, Color(0x1AFFFFFF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rounds.keys.forEach { roundNum ->
                        val isSelected = selectedMobileRound == roundNum
                        val matchesInThisRound = rounds[roundNum] ?: emptyList()
                        val hasReady = matchesInThisRound.any { it.isReady }
                        val isAllDone = matchesInThisRound.isNotEmpty() && matchesInThisRound.all { it.isFinished }

                        Surface(
                            onClick = { selectedMobileRound = roundNum },
                            shape = RoundedCornerShape(9.dp),
                            color = if (isSelected) Color(0x2BFFFFFF) else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = getRoundTitle(roundNum),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0x80FFFFFF),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (hasReady && !isAllDone) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(PrimaryOrange, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Umschalt-Option auf vollständigen Baum
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { mobileViewMode = 1 },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Vollständigen Baum ansehen →",
                        color = PrimaryOrange,
                        fontSize = 11.sp
                    )
                }
            }

            val currentMatches = rounds[selectedMobileRound] ?: emptyList()
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(currentMatches) { match ->
                    BracketMatchCard(
                        match = match,
                        onClick = {
                            if (match.isReady || (match.isFinished && match.player1 != null && match.player2 != null)) {
                                onMatchClick(match)
                            }
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

/**
 * 1dp feine Bracket-Verbindungslinien zwischen Vorrunde und Folgerunde.
 * Verwendet exakte Binärbaum-Koordinaten, sodass jeder Ast pixelgenau auf die Mitte der Karten trifft.
 */
@Composable
fun BracketConnectorCanvas(
    leftRoundIndex: Int,
    leftMatches: List<TournamentMatch>,
    slotHeightR1: Dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val midX = w / 2f
        val strokeWidth = 1.dp.toPx()
        val defaultLineColor = Color(0x26FFFFFF)
        val winnerLineColor = PrimaryOrange

        val s1 = slotHeightR1.toPx()
        val stepR = (1 shl leftRoundIndex) * s1 // 2^leftRoundIndex * s1

        val leftCount = leftMatches.size
        for (j in 0 until (leftCount / 2)) {
            val idx1 = j * 2
            val idx2 = j * 2 + 1
            val m1 = leftMatches[idx1]
            val m2 = leftMatches.getOrNull(idx2)

            val y1 = (idx1 + 0.5f) * stepR
            val y2 = if (m2 != null) (idx2 + 0.5f) * stepR else y1
            val yMid = (y1 + y2) / 2f

            val m1Won = m1.isFinished && m1.winner != null
            val m2Won = m2?.isFinished == true && m2.winner != null

            // Oberer Ast
            drawLine(
                color = if (m1Won) winnerLineColor else defaultLineColor,
                start = Offset(0f, y1),
                end = Offset(midX, y1),
                strokeWidth = strokeWidth
            )

            if (m2 != null) {
                // Unterer Ast
                drawLine(
                    color = if (m2Won) winnerLineColor else defaultLineColor,
                    start = Offset(0f, y2),
                    end = Offset(midX, y2),
                    strokeWidth = strokeWidth
                )

                // Vertikaler Steg
                drawLine(
                    color = if (m1Won) winnerLineColor else defaultLineColor,
                    start = Offset(midX, y1),
                    end = Offset(midX, yMid),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = if (m2Won) winnerLineColor else defaultLineColor,
                    start = Offset(midX, yMid),
                    end = Offset(midX, y2),
                    strokeWidth = strokeWidth
                )

                // Auslauf zum Folge-Match in der rechten Spalte
                val outgoingColor = if (m1Won || m2Won) winnerLineColor else defaultLineColor
                drawLine(
                    color = outgoingColor,
                    start = Offset(midX, yMid),
                    end = Offset(w, yMid),
                    strokeWidth = strokeWidth
                )
            }
        }
    }
}

/**
 * 1dp Linie vom Finale zum Champion-Podium.
 */
@Composable
fun FinalToChampionConnector(
    finalY: Dp,
    hasChampion: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val y = finalY.toPx()
        drawLine(
            color = if (hasChampion) PrimaryOrange else Color(0x26FFFFFF),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.dp.toPx()
        )
    }
}

/**
 * Champion Kachel am rechten Ende des Turnierbaums.
 */
@Composable
fun ChampionPodiumCard(
    champion: TournamentPlayer,
    isLarge: Boolean = false,
    isExtraLarge: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(if (isExtraLarge) 22.dp else 18.dp),
        color = Color(0x14FFFFFF),
        border = BorderStroke(0.5.dp, PrimaryOrange.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(if (isExtraLarge) 32.dp else if (isLarge) 24.dp else 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(if (isExtraLarge) 76.dp else if (isLarge) 64.dp else 50.dp)
                    .background(Color(0x26FF8800), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_trophy),
                    contentDescription = "Champion Trophäe",
                    tint = PrimaryOrange,
                    modifier = Modifier.size(if (isExtraLarge) 40.dp else if (isLarge) 34.dp else 26.dp)
                )
            }
            Spacer(modifier = Modifier.height(if (isExtraLarge) 14.dp else if (isLarge) 12.dp else 10.dp))
            Text(
                text = "CHAMPION",
                color = PrimaryOrange,
                fontSize = if (isExtraLarge) 13.sp else if (isLarge) 11.5.sp else 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = champion.name,
                color = Color.White,
                fontSize = if (isExtraLarge) 22.sp else if (isLarge) 18.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Apple Sports-inspirierte Match-Kachel (zweizeilig, 0.5dp Divider, Score-Kapseln).
 */
@Composable
fun BracketMatchCard(
    match: TournamentMatch,
    onClick: () -> Unit,
    isLarge: Boolean = false,
    isExtraLarge: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isReady = match.isReady
    val isFinished = match.isFinished

    val borderColor = when {
        isReady -> PrimaryOrange.copy(alpha = 0.5f)
        isFinished -> Color(0x22FFFFFF)
        else -> Color(0x14FFFFFF)
    }

    val backgroundColor = when {
        isReady -> Color(0x14FF8800)
        isFinished -> Color(0x0CFFFFFF)
        else -> Color(0x07FFFFFF)
    }

    val canInteract = isReady || (isFinished && match.player1 != null && match.player2 != null)

    Surface(
        onClick = onClick,
        enabled = canInteract,
        shape = RoundedCornerShape(if (isExtraLarge) 18.dp else 14.dp),
        color = backgroundColor,
        border = BorderStroke(if (isReady) 1.dp else 0.5.dp, borderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (isExtraLarge) 20.dp else if (isLarge) 15.dp else 12.dp,
                vertical = if (isExtraLarge) 15.dp else if (isLarge) 12.dp else 9.dp
            )
        ) {
            // Status-Zeile
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spiel ${match.matchIndexInRound + 1}",
                    fontSize = if (isExtraLarge) 13.sp else if (isLarge) 11.5.sp else 10.sp,
                    color = Color(0x66FFFFFF),
                    fontWeight = FontWeight.Medium
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    when {
                        match.isBye -> {
                            Text(
                                text = "Freilos",
                                fontSize = if (isExtraLarge) 13.sp else if (isLarge) 11.5.sp else 10.sp,
                                color = Color(0x66FFFFFF),
                                fontWeight = FontWeight.Normal
                            )
                        }
                        isFinished -> {
                            Text(
                                text = "Beendet",
                                fontSize = if (isExtraLarge) 13.sp else if (isLarge) 11.5.sp else 10.sp,
                                color = Color(0x80FFFFFF),
                                fontWeight = FontWeight.Normal
                            )
                        }
                        isReady -> {
                            Box(
                                modifier = Modifier
                                    .size(if (isExtraLarge) 8.5.dp else if (isLarge) 7.dp else 6.dp)
                                    .background(PrimaryOrange, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Bereit",
                                fontSize = if (isExtraLarge) 13.sp else if (isLarge) 11.5.sp else 10.sp,
                                color = PrimaryOrange,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        else -> {
                            Text(
                                text = "Wartend",
                                fontSize = if (isExtraLarge) 13.sp else if (isLarge) 11.5.sp else 10.sp,
                                color = Color(0x40FFFFFF),
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isExtraLarge) 11.dp else if (isLarge) 8.dp else 6.dp))

            // Spieler 1
            PlayerSlotRow(
                player = match.player1,
                fallbackText = if (match.isBye && match.player1 == null) "Freilos" else "TBD",
                score = if (isFinished) match.player1Result.legsWon else null,
                isWinner = isFinished && match.winner?.id == match.player1?.id && match.player1 != null,
                isLarge = isLarge,
                isExtraLarge = isExtraLarge
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = if (isExtraLarge) 8.dp else if (isLarge) 6.dp else 5.dp),
                thickness = if (isExtraLarge) 0.75.dp else 0.5.dp,
                color = Color(0x14FFFFFF)
            )

            // Spieler 2
            PlayerSlotRow(
                player = match.player2,
                fallbackText = if (match.isBye && match.player2 == null) "Freilos" else "TBD",
                score = if (isFinished) match.player2Result.legsWon else null,
                isWinner = isFinished && match.winner?.id == match.player2?.id && match.player2 != null,
                isLarge = isLarge,
                isExtraLarge = isExtraLarge
            )
        }
    }
}

@Composable
private fun PlayerSlotRow(
    player: TournamentPlayer?,
    fallbackText: String,
    score: Int?,
    isWinner: Boolean,
    isLarge: Boolean = false,
    isExtraLarge: Boolean = false
) {
    val name = player?.name ?: fallbackText
    val isKnown = player != null

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isWinner) Color(0x33FF8800) else Color(0x14FFFFFF),
                modifier = Modifier.size(if (isExtraLarge) 32.dp else if (isLarge) 24.dp else 18.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val initial = if (isKnown && name.isNotBlank()) name.firstOrNull()?.uppercaseChar()?.toString() ?: "•" else "•"
                    Text(
                        text = initial,
                        fontSize = if (isExtraLarge) 14.sp else if (isLarge) 11.sp else 9.sp,
                        fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Medium,
                        color = if (isWinner) PrimaryOrange else Color(0x99FFFFFF)
                    )
                }
            }

            Spacer(modifier = Modifier.width(if (isExtraLarge) 12.dp else if (isLarge) 9.dp else 7.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isWinner) FontWeight.Bold else FontWeight.SemiBold,
                    color = when {
                        isWinner -> Color.White
                        isKnown -> Color(0xCCFFFFFF)
                        else -> Color(0x4DFFFFFF)
                    },
                    fontSize = if (isExtraLarge) 17.sp else if (isLarge) 15.sp else 13.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (score != null) {
            Surface(
                shape = RoundedCornerShape(if (isExtraLarge) 8.dp else if (isLarge) 7.dp else 5.dp),
                color = if (isWinner) Color(0x26FF8800) else Color(0x0FFFFFFF),
                modifier = Modifier.padding(start = 6.dp)
            ) {
                Text(
                    text = score.toString(),
                    fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                    color = if (isWinner) PrimaryOrange else Color(0xB3FFFFFF),
                    fontSize = if (isExtraLarge) 16.sp else if (isLarge) 14.sp else 12.sp,
                    modifier = Modifier.padding(
                        horizontal = if (isExtraLarge) 10.dp else if (isLarge) 8.dp else 6.dp,
                        vertical = if (isExtraLarge) 4.dp else if (isLarge) 3.dp else 2.dp
                    )
                )
            }
        }
    }
}
