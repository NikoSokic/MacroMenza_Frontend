package com.niko.macromenza.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.navigation.NavController


@Composable
fun ProfilScreen(
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Profil",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        ProfileAvatar(
            initials = "NS"
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Niko Sokić",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        TextButton(
            onClick = {
                navController.navigate("uredi_profil")
            }
        ) {
            Text("uredi profil")
        }

        Spacer(modifier = Modifier.height(18.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Ciljevi",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            GoalCard(
                title = "Održavanje težine",
                subtitle = "2.400 kcal dnevno"
            )

            Spacer(modifier = Modifier.height(18.dp))

            ProfileMenuItem(
                title = "Postavke ciljeva",
                iconType = ProfileIconType.Goal,
                onClick = {
                    navController.navigate("postavke_ciljeva")
                }
            )

            ProfileMenuItem(
                title = "Moje mjere",
                iconType = ProfileIconType.Measurements,
                onClick = {
                    // kasnije: ekran mjera
                }
            )

            ProfileMenuItem(
                title = "Postavke aplikacije",
                iconType = ProfileIconType.Settings,
                onClick = {
                    // kasnije: ekran postavki aplikacije
                }
            )

            ProfileMenuItem(
                title = "Odjava",
                iconType = ProfileIconType.Logout,
                onClick = {
                    // kasnije: logout
                }
            )
        }
    }
}

@Composable
fun ProfileAvatar(
    initials: String
) {
    Surface(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun GoalCard(
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

enum class ProfileIconType {
    Goal,
    Measurements,
    Settings,
    Logout
}

@Composable
fun ProfileMenuItem(
    title: String,
    iconType: ProfileIconType,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (iconType) {
                            ProfileIconType.Goal -> Icons.Default.Flag
                            ProfileIconType.Measurements -> Icons.Default.Straighten
                            ProfileIconType.Settings -> Icons.Default.Settings
                            ProfileIconType.Logout -> Icons.AutoMirrored.Filled.ExitToApp
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}