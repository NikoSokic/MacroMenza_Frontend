package com.niko.macromenza.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.niko.macromenza.ui.theme.MacroGreen
import com.niko.macromenza.ui.theme.MacroLightGreen
import com.niko.macromenza.ui.theme.MacroText
import com.niko.macromenza.ui.theme.MacroTextSecondary

@Composable
fun KonzumacijaScreen(
    navController: NavController
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // Pozadinska dekoracija
        Box(
            modifier = Modifier
                .size(230.dp)
                .offset(x = 150.dp, y = (-110).dp)
                .clip(CircleShape)
                .background(
                    MacroLightGreen.copy(alpha = 0.08f)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 28.dp, bottom = 110.dp)
        ) {

            Text(
                text = "Što jedeš?",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MacroText
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Odaberi vrstu obroka koju želiš evidentirati.",
                style = MaterialTheme.typography.bodyLarge,
                color = MacroTextSecondary
            )

            Spacer(modifier = Modifier.height(30.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MealSelectionCard(
                    title = "Doručak",
                    subtitle = "Započni dan",
                    tipObroka = "DORUCAK",
                    navController = navController,
                    modifier = Modifier.weight(1f)
                )

                MealSelectionCard(
                    title = "Ručak",
                    subtitle = "Glavni obrok",
                    tipObroka = "RUCAK",
                    navController = navController,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MealSelectionCard(
                    title = "Večera",
                    subtitle = "Večernji obrok",
                    tipObroka = "VECERA",
                    navController = navController,
                    modifier = Modifier.weight(1f)
                )

                MealSelectionCard(
                    title = "Užina",
                    subtitle = "Nešto između",
                    tipObroka = "UZINA",
                    navController = navController,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MealSelectionCard(
    title: String,
    subtitle: String,
    tipObroka: String,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = {
            navController.navigate("unos_obroka/$tipObroka")
        },
        modifier = modifier.height(165.dp),
        shape = RoundedCornerShape(24.dp),
        color = MacroLightGreen.copy(alpha = 0.13f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(
                        MacroGreen.copy(alpha = 0.14f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = MacroGreen,
                    modifier = Modifier.size(27.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                color = MacroText,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                color = MacroTextSecondary,
                fontSize = 12.sp
            )
        }
    }
}