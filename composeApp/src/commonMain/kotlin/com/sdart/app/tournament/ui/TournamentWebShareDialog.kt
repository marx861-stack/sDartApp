package com.sdart.app.tournament.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdart.app.tournament.web.QrCodeView
import com.sdart.app.tournament.web.TournamentWebServer
import com.sdart.app.ui.theme.PrimaryOrange
import org.jetbrains.compose.resources.stringResource
import sdartapp.composeapp.generated.resources.*

@Composable
fun TournamentWebShareDialog(
    webServer: TournamentWebServer,
    onDismiss: () -> Unit
) {
    var allowSubmission by remember { mutableStateOf(webServer.allowScoreSubmission) }
    var requireApproval by remember { mutableStateOf(webServer.requireHostApproval) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(Res.string.tour_share_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.tour_share_subtitle),
                    color = Color(0x99FFFFFF),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // QR-Code
                val url = webServer.localUrl
                if (url.isNotBlank()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(210.dp)
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(10.dp)
                    ) {
                        QrCodeView(
                            content = url,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(210.dp)
                            .background(Color(0x14FFFFFF), RoundedCornerShape(16.dp))
                    ) {
                        Text(stringResource(Res.string.tour_share_starting), color = Color.Gray, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Klickbare / Lesbare URL-Pille
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x14FFFFFF),
                    border = BorderStroke(0.5.dp, Color(0x26FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = url.ifBlank { "http://127.0.0.1:8080" },
                            color = PrimaryOrange,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Optionen: Schalter
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x0AFFFFFF),
                    border = BorderStroke(0.5.dp, Color(0x1FFFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(Res.string.tour_share_allow), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(stringResource(Res.string.tour_share_allow_desc), color = Color(0x80FFFFFF), fontSize = 11.sp)
                            }
                            Switch(
                                checked = allowSubmission,
                                onCheckedChange = {
                                    allowSubmission = it
                                    webServer.allowScoreSubmission = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = PrimaryOrange
                                )
                            )
                        }

                        if (allowSubmission) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0x14FFFFFF))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(Res.string.tour_share_approval), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(stringResource(Res.string.tour_share_approval_desc), color = Color(0x80FFFFFF), fontSize = 11.sp)
                                }
                                Switch(
                                    checked = requireApproval,
                                    onCheckedChange = {
                                        requireApproval = it
                                        webServer.requireHostApproval = it
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = PrimaryOrange
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(Res.string.tour_share_done), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}
