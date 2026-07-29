package com.niko.macromenza.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.niko.macromenza.session.ThemeManager
import com.niko.macromenza.ui.components.screenBottomPadding
import com.niko.macromenza.ui.components.screenTopPadding
import com.niko.macromenza.ui.theme.AppTheme
import com.niko.macromenza.ui.theme.MacroGreen
import com.niko.macromenza.ui.theme.MacroLightGreen

import kotlinx.coroutines.launch

@Composable
fun PostavkeAplikacijeScreen(
    navController: NavController
) {
    val context = LocalContext.current

    val themeManager = remember {
        ThemeManager(context)
    }

    val scope = rememberCoroutineScope()

    val spremljenaTema by
    themeManager.theme.collectAsState(
        initial = AppTheme.SYSTEM
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // Dekoracija
        Box(
            modifier = Modifier
                .size(230.dp)
                .offset(
                    x = 190.dp,
                    y = (-110).dp
                )
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(horizontal = 22.dp)
                .padding(
                    top = screenTopPadding(),
                    bottom = screenBottomPadding()
                )
        ) {

            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    onClick = {
                        navController.popBackStack()
                    },
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(
                        alpha = 0.16f
                    )
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Nazad",
                            tint = MacroGreen
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.width(14.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Postavke aplikacije",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "Prilagodi MacroMenzu sebi",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            // TEMA
            AppSettingsCard(
                title = "Tema aplikacije",
                subtitle = "Odaberi izgled aplikacije",
                icon = Icons.Default.Palette
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    ThemeOption(
                        title = "Sustav",
                        icon = Icons.Default.SettingsSuggest,
                        selected =
                            spremljenaTema ==
                                    AppTheme.SYSTEM,
                        onClick = {
                            scope.launch {
                                themeManager.saveTheme(
                                    AppTheme.SYSTEM
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    ThemeOption(
                        title = "Svijetla",
                        icon = Icons.Default.LightMode,
                        selected =
                            spremljenaTema ==
                                    AppTheme.LIGHT,
                        onClick = {
                            scope.launch {
                                themeManager.saveTheme(
                                    AppTheme.LIGHT
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    ThemeOption(
                        title = "Tamna",
                        icon = Icons.Default.DarkMode,
                        selected =
                            spremljenaTema ==
                                    AppTheme.DARK,
                        onClick = {
                            scope.launch {
                                themeManager.saveTheme(
                                    AppTheme.DARK
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            // STATUS
            AppSettingsCard(
                title = "Status sustava",
                subtitle = "Servisi koje MacroMenza koristi",
                icon = Icons.Default.CloudDone
            ) {

                StatusRow(
                    title = "Backend",
                    value = "Render",
                    status = "Online"
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                HorizontalDivider(
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.10f
                        )
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                StatusRow(
                    title = "Baza podataka",
                    value = "Supabase PostgreSQL",
                    status = "Povezano"
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                HorizontalDivider(
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.10f
                        )
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                StatusRow(
                    title = "Autentifikacija",
                    value = "Supabase Auth",
                    status = "Aktivno"
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            // O APLIKACIJI
            AppSettingsCard(
                title = "O aplikaciji",
                subtitle = "Informacije o MacroMenzi",
                icon = Icons.Default.Info
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Surface(
                        modifier = Modifier.size(58.dp),
                        shape = RoundedCornerShape(18.dp),
                        color =
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.20f
                            )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {

                            Text(
                                text = "M",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = MacroGreen
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.width(14.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "MacroMenza",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "Verzija 1.0",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style =
                                MaterialTheme.typography
                                    .bodySmall,
                            maxLines = 1
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Text(
                    text =
                        "Aplikacija za praćenje prehrane, kalorija i makronutrijenata u menzi.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style =
                        MaterialTheme.typography.bodyMedium
                )

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                InfoRow(
                    title = "Platforma",
                    value = "Android"
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                InfoRow(
                    title = "Aplikacija",
                    value = "MacroMenza 1.0"
                )
            }
        }
    }
}

@Composable
private fun AppSettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(15.dp),
                    color =
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.18f
                        )
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MacroGreen,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )

                    Text(
                        text = subtitle,
                        style =
                            MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            content()
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(
            min = 90.dp
        ),
        shape = RoundedCornerShape(18.dp),
        color =
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(
                    alpha = 0.22f
                )
            } else {
                MaterialTheme.colorScheme.background
            },
        border =
            if (selected) {
                androidx.compose.foundation.BorderStroke(
                    width = 1.5.dp,
                    color = MacroGreen
                )
            } else {
                androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.15f
                        )
                )
            }
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 6.dp,
                    vertical = 14.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint =
                    if (selected) {
                        MacroGreen
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.size(25.dp)
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = title,
                fontWeight =
                    if (selected) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                color =
                    if (selected) {
                        MacroGreen
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatusRow(
    title: String,
    value: String,
    status: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(
            modifier = Modifier.width(10.dp)
        )

        Surface(
            shape = RoundedCornerShape(50),
            color =
                MaterialTheme.colorScheme.primary.copy(
                    alpha = 0.22f
                )
        ) {

            Row(
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 6.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(MacroGreen)
                )

                Spacer(
                    modifier = Modifier.width(6.dp)
                )

                Text(
                    text = status,
                    color = MacroGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        Text(
            text = value,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}