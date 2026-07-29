package com.niko.macromenza.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.niko.macromenza.ui.theme.MacroGreen

@Composable
fun WelcomeScreen(
    navController: NavController
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // Na nižim ekranima automatski koristimo manje razmake
        val compactScreen = maxHeight < 750.dp

        val topSpace = if (compactScreen) 28.dp else 60.dp
        val sectionSpace = if (compactScreen) 18.dp else 28.dp
        val logoSize = if (compactScreen) 72.dp else 86.dp
        val titleSize = if (compactScreen) 36.sp else 42.sp

        // Dekoracija gore lijevo
        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = (-100).dp, y = (-90).dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
        )

        // Dekoracija dolje desno
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 120.dp, y = 110.dp)
                .clip(CircleShape)
                .background(
                    MacroGreen.copy(alpha = 0.08f)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(topSpace))

            // Privremeni logo
            Box(
                modifier = Modifier
                    .size(logoSize)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "M",
                    fontSize = if (compactScreen) 36.sp else 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = MacroGreen
                )
            }

            Spacer(
                modifier = Modifier.height(
                    if (compactScreen) 18.dp else 28.dp
                )
            )

            Text(
                text = "Dobrodošao u",
                fontSize = if (compactScreen) 26.sp else 30.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("Macro")
                    }

                    withStyle(
                        SpanStyle(
                            color = MacroGreen,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("Menza")
                    }
                },
                fontSize = titleSize,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MacroGreen)
            )

            Spacer(modifier = Modifier.height(sectionSpace))

            Text(
                text = "Prati prehranu i makronutrijente u menzi na jednostavan način.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(sectionSpace))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FeatureItem(
                    icon = Icons.Default.BarChart,
                    title = "Prati unos\ni napredak"
                )

                FeatureItem(
                    icon = Icons.Default.TrackChanges,
                    title = "Ostvari svoje\nciljeve"
                )

                FeatureItem(
                    icon = Icons.Default.Restaurant,
                    title = "Pametan odabir\nu menzi"
                )
            }

            Spacer(
                modifier = Modifier.height(
                    if (compactScreen) 30.dp else 50.dp
                )
            )

            Button(
                onClick = {
                    navController.navigate("register")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MacroGreen,
                    contentColor = Color.White
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Registriraj se",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    navController.navigate("login")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.5.dp,
                    color = MacroGreen
                )
            ) {
                Text(
                    text = "Prijavi se",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MacroGreen
                )
            }

            // Da zadnji gumb nikad ne završi ispod navigation bara
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {

        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MacroGreen,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}