package com.niko.macromenza.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niko.macromenza.ui.theme.MacroGreen
import com.niko.macromenza.ui.theme.MacroLightGreen

@Composable
fun SplashScreen() {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // Dekoracija gore desno
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(
                    x = 190.dp,
                    y = (-130).dp
                )
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                )
        )

        // Dekoracija dolje lijevo
        Box(
            modifier = Modifier
                .size(230.dp)
                .align(Alignment.BottomStart)
                .offset(
                    x = (-120).dp,
                    y = 100.dp
                )
                .clip(CircleShape)
                .background(
                    MacroGreen.copy(alpha = 0.06f)
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Privremeni logo
            Surface(
                modifier = Modifier.size(100.dp),
                shape = RoundedCornerShape(30.dp),
                color = MacroGreen,
                shadowElevation = 8.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "M",
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Text(
                text = "MacroMenza",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Text(
                text = "Pametnije prati svoju prehranu",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(36.dp)
            )

            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = MacroGreen,
                strokeWidth = 3.dp
            )
        }

        Text(
            text = "Prehrana pod kontrolom",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 42.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = 0.65f
            )
        )
    }
}