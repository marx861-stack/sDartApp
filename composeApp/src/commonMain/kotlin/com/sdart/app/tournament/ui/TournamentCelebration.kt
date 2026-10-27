package com.sdart.app.tournament.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdart.app.tournament.TournamentPlayer
import com.sdart.app.ui.theme.PrimaryOrange
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sdartapp.composeapp.generated.resources.*

@Composable
fun TournamentCelebrationCard(
    winner: TournamentPlayer,
    tournamentName: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF1C1C1E),
        border = BorderStroke(0.5.dp, Color(0x33FFFFFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x2BFF8800), Color.Transparent),
                        radius = 420f
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(Color(0x22FF8800), CircleShape)
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_trophy),
                    contentDescription = stringResource(Res.string.tour_celebration_trophy_cd),
                    tint = PrimaryOrange,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(Res.string.tour_celebration_title),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = PrimaryOrange,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.8.sp,
                    fontSize = 11.sp
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = winner.name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(Res.string.tour_celebration_congrats, tournamentName),
                color = Color(0x99FFFFFF),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(
                    text = stringResource(Res.string.tour_celebration_back),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
