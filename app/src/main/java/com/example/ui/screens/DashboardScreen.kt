package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.TextStyle
import com.example.data.*
import com.example.ui.*
import com.example.ui.components.MapCanvas
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TrackingViewModel,
    modifier: Modifier = Modifier
) {
    val children by viewModel.children.collectAsStateWithLifecycle()
    val safeZones by viewModel.safeZones.collectAsStateWithLifecycle()
    val pointsOfInterest by viewModel.pointsOfInterest.collectAsStateWithLifecycle()
    val panicAlerts by viewModel.panicAlerts.collectAsStateWithLifecycle()
    val selectedChildId by viewModel.selectedChildId.collectAsStateWithLifecycle()
    val selectedChildHistory by viewModel.selectedChildHistory.collectAsStateWithLifecycle()
    val selectedChildBatteryLogs by viewModel.selectedChildBatteryLogs.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val isSimulationRunning by viewModel.isSimulationRunning.collectAsStateWithLifecycle()
    val currentUserRole by viewModel.currentUserRole.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Simulador, 1: Zonas Seguras, 2: Alertas & Rutas
    val selectedChild = children.find { it.id == selectedChildId }
    val activePanicAlert = panicAlerts.find { !it.isResolved }

    // Pulsing Red Gradient Brush for Panic Alert Top Banner
    val infiniteTransition = rememberInfiniteTransition(label = "panic_pulsar")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "panic_pulse_alpha"
    )

    when (currentUserRole) {
        TrackingViewModel.UserRole.SPLASH -> {
            SplashScreenSelector(
                onChooseRole = { role -> viewModel.selectRole(role) }
            )
        }
        TrackingViewModel.UserRole.NINO -> {
            ChildDeviceScreen(
                viewModel = viewModel,
                children = children,
                selectedChildId = selectedChildId,
                selectedChild = selectedChild,
                onBackToSplash = { viewModel.selectRole(TrackingViewModel.UserRole.SPLASH) }
            )
        }
        TrackingViewModel.UserRole.PADRE -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FamilyRestroom,
                                    contentDescription = "Rastreador Familiar",
                                    tint = com.example.ui.theme.HighDensityPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "FamiGuard",
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.HighDensityText
                                )
                            }
                        },
                        actions = {
                            Box(modifier = Modifier.padding(end = 8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.selectRole(TrackingViewModel.UserRole.SPLASH) },
                                        modifier = Modifier.testTag("switch_to_splash")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Cambiar Rol",
                                            tint = com.example.ui.theme.HighDensityPrimary
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(
                                                    if (isSimulationRunning) Color(0xFF2E7D32) else Color.Gray,
                                                    CircleShape
                                                )
                                        )
                                        Text(
                                            text = if (isSimulationRunning) "Simulando" else "Pausado",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSimulationRunning) Color(0xFF2E7D32) else com.example.ui.theme.HighDensitySecondaryText,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = com.example.ui.theme.HighDensityBg
                        )
                    )
                },
                containerColor = com.example.ui.theme.HighDensityBg,
                modifier = modifier
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
            // ---- PANIC WARNING TRIGGER OVERLAY ----
            AnimatedVisibility(
                visible = activePanicAlert != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (activePanicAlert != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("panic_alert_banner"),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF8B0000).copy(alpha = alphaAnim),
                                            Color(0xFFFF0000).copy(alpha = alphaAnim)
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NewReleases,
                                        contentDescription = "Emergencia de Pánico",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = "🚨 ALERTA DE PÁNICO ACTIVA 🚨",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                Text(
                                    text = "El botón de pánico fue presionado por ${activePanicAlert.childName}. Coordenadas: [${String.format("%.4f", activePanicAlert.latitude)}, ${String.format("%.4f", activePanicAlert.longitude)}]. ¡Se requiere atención inmediata!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )

                                Button(
                                    onClick = { viewModel.resolvePanic(activePanicAlert.id, activePanicAlert.childId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Red),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("resolve_panic_button")
                                ) {
                                    Text(
                                        "RESOLVER Y DESACTIVAR ALARMA",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ---- CHILD IN FOCUS CARD SELECTOR CHIPS ----
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.HighDensityBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Niños Monitoreados",
                        style = MaterialTheme.typography.titleSmall,
                        color = com.example.ui.theme.HighDensityText,
                        fontWeight = FontWeight.Bold
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(children) { child ->
                            val isFocused = child.id == selectedChildId

                            Surface(
                                modifier = Modifier
                                    .testTag("child_chip_${child.id}")
                                    .clickable { viewModel.selectChild(child.id) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isFocused) com.example.ui.theme.HighDensityPrimaryContainer else com.example.ui.theme.HighDensityBg,
                                border = if (isFocused) {
                                    androidx.compose.foundation.BorderStroke(1.5.dp, com.example.ui.theme.HighDensityPrimary)
                                } else {
                                    androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.HighDensityBorder)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Animated indicator dot or alarm alert
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(
                                                if (child.isPanicActive) Color.Red else Color(child.avatarColor),
                                                CircleShape
                                            )
                                    )

                                    Column {
                                        Text(
                                            text = child.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isFocused) com.example.ui.theme.HighDensityOnPrimaryContainer else com.example.ui.theme.HighDensityText
                                        )
                                        Text(
                                            text = if (child.isPanicActive) "¡PÁNICO!" else child.currentZoneStatus,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (child.isPanicActive) Color.Red else com.example.ui.theme.HighDensitySecondaryText,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---- REAL-TIME MAP CANVAS VIEW ----
            Text(
                text = "Mapa de Ubicación en Tiempo Real",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = com.example.ui.theme.HighDensityText
            )

            MapCanvas(
                children = children,
                selectedChildId = selectedChildId,
                safeZones = safeZones,
                historyPoints = selectedChildHistory,
                pointsOfInterest = pointsOfInterest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )

            // ---- CONTROLS / DETAILS NAVIGATION TABS ----
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = com.example.ui.theme.HighDensityPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Text(
                            text = "Simulador",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (activeTab == 0) com.example.ui.theme.HighDensityPrimary else com.example.ui.theme.HighDensitySecondaryText
                        )
                    }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Text(
                            text = "Zonas Seguras",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (activeTab == 1) com.example.ui.theme.HighDensityPrimary else com.example.ui.theme.HighDensitySecondaryText
                        )
                    }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = {
                        Text(
                            text = "Historial & Alertas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (activeTab == 2) com.example.ui.theme.HighDensityPrimary else com.example.ui.theme.HighDensitySecondaryText
                        )
                    }
                )
            }

            // ---- TAB CONTENTS ----
            when (activeTab) {
                0 -> SimulatedControlsPanel(
                    viewModel = viewModel,
                    selectedChild = selectedChild,
                    isSimulationRunning = isSimulationRunning
                )
                1 -> SafeZonesAdminPanel(
                    viewModel = viewModel,
                    safeZones = safeZones,
                    pointsOfInterest = pointsOfInterest
                )
                2 -> HistoryAndAlertsPanel(
                    viewModel = viewModel,
                    selectedChild = selectedChild,
                    historyPoints = selectedChildHistory,
                    notifications = notifications,
                    batteryLogs = selectedChildBatteryLogs
                )
            }
        }
    }
}
}
}

// ----------------------------------------------------
// TAB 0: CONTROLES DE SIMULACIÓN Y BOTONES DE PÁNICO
// ----------------------------------------------------
@Composable
fun SimulatedControlsPanel(
    viewModel: TrackingViewModel,
    selectedChild: Child?,
    isSimulationRunning: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.HighDensityBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Simulador de Geolocalización Familiar",
                style = MaterialTheme.typography.titleSmall,
                color = com.example.ui.theme.HighDensityPrimary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Use estos controles para encender el caminar automático de los niños por la calle o forzar eventos de geofencing de prueba.",
                style = MaterialTheme.typography.bodySmall,
                color = com.example.ui.theme.HighDensitySecondaryText
            )

            if (selectedChild == null) {
                Text(
                    text = "Seleccione un niño arriba para simular sus acciones.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = com.example.ui.theme.HighDensitySecondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                )
            } else {
                Text(
                    text = "Niño activo: ${selectedChild.name}",
                    fontWeight = FontWeight.Bold,
                    color = com.example.ui.theme.HighDensityText,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Row of main simulation switches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.toggleSimulation() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSimulationRunning) Color(0xFFC93B2B) else com.example.ui.theme.HighDensityPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_simulation_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isSimulationRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Iniciar"
                            )
                            Text(
                                if (isSimulationRunning) "Pausar Auto" else "Movimiento Auto",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.manuallyStepSimulation() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = com.example.ui.theme.HighDensityPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, com.example.ui.theme.HighDensityPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("step_simulation_button"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSimulationRunning
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = "Manual Walk",
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Paso Manual", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = com.example.ui.theme.HighDensityBorder, thickness = 1.dp)

                // Panic Button Simulation
                Text(
                    text = "Botón de Pánico Físico (Simular)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = com.example.ui.theme.HighDensityText
                )

                Button(
                    onClick = { viewModel.triggerPanic() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFBA1A1A),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("panic_trigger_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Botón de pánico",
                            tint = Color.White
                        )
                        Text(
                            "¡SOS - BOTÓN DE PÁNICO!",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }

                HorizontalDivider(color = com.example.ui.theme.HighDensityBorder, thickness = 1.dp)

                // Geofencing Test Tools (Teleporting)
                Text(
                    text = "Acciones de Geofencing Forzado",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = com.example.ui.theme.HighDensityText
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.teleportChild(selectedChild.id, outOfZones = false) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF386A20),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("teleport_inside_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Forzar Zona Segura", fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }

                    Button(
                        onClick = { viewModel.teleportChild(selectedChild.id, outOfZones = true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFBA1A1A),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("teleport_outside_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Forzar Zona Peligro", fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 1: GESTIÓN DE ZONAS SEGURAS Y PUNTOS DE INTERÉS
// ----------------------------------------------------
@Composable
fun SafeZonesAdminPanel(
    viewModel: TrackingViewModel,
    safeZones: List<SafeZone>,
    pointsOfInterest: List<PointOfInterest>
) {
    var zoneName by remember { mutableStateOf("") }
    var zoneType by remember { mutableStateOf("Hogar") } // Hogar, Escuela, Parque, Otros
    var radiusMeters by remember { mutableStateOf(100f) }
    var showAddZoneForm by remember { mutableStateOf(false) }

    // POI States
    var poiName by remember { mutableStateOf("") }
    var poiType by remember { mutableStateOf("Actividad Extraescolar") } // Casa, Escuela, Actividad Extraescolar, Otro
    var poiLat by remember { mutableStateOf(40.4180f) }
    var poiLng by remember { mutableStateOf(-3.7050f) }
    var showAddPoiForm by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // --- SECCIÓN 1: GEOFENCE ZONAS SEGURAS ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.HighDensityBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Zonas de Seguridad Geocercadas",
                        style = MaterialTheme.typography.titleSmall,
                        color = com.example.ui.theme.HighDensityPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = { showAddZoneForm = !showAddZoneForm }) {
                        Icon(
                            imageVector = if (showAddZoneForm) Icons.Default.Close else Icons.Default.AddCircle,
                            contentDescription = "Agregar Zona",
                            tint = com.example.ui.theme.HighDensityPrimary
                        )
                    }
                }

                // Collapse Form for Geofences
                AnimatedVisibility(visible = showAddZoneForm) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.HighDensityBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.HighDensityBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Configurar Nueva Zona Segura",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.HighDensityText
                            )

                            OutlinedTextField(
                                value = zoneName,
                                onValueChange = { zoneName = it },
                                label = { Text("Nombre de la Zona", color = com.example.ui.theme.HighDensitySecondaryText) },
                                textStyle = TextStyle(color = com.example.ui.theme.HighDensityText),
                                modifier = Modifier.fillMaxWidth().testTag("zone_name_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = com.example.ui.theme.HighDensityPrimary,
                                    unfocusedBorderColor = com.example.ui.theme.HighDensityBorder
                                ),
                                singleLine = true
                            )

                            // Selector Tipo de Zona
                            Text(
                                text = "Tipo de Zona:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.HighDensityText
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("Hogar", "Escuela", "Parque", "Otros").forEach { entry ->
                                    val selected = zoneType == entry
                                    Button(
                                        onClick = { zoneType = entry },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selected) com.example.ui.theme.HighDensityPrimary else com.example.ui.theme.HighDensityBg,
                                            contentColor = if (selected) Color.White else com.example.ui.theme.HighDensitySecondaryText
                                        ),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.HighDensityBorder),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(entry, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Radio en Metros
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Radio Límite:", fontSize = 12.sp, color = com.example.ui.theme.HighDensitySecondaryText)
                                    Text("${radiusMeters.toInt()} metros", fontWeight = FontWeight.Bold, color = com.example.ui.theme.HighDensityPrimary, fontSize = 12.sp)
                                }
                                Slider(
                                    value = radiusMeters,
                                    onValueChange = { radiusMeters = it },
                                    valueRange = 30f..300f,
                                    modifier = Modifier.testTag("zone_radius_slider")
                                )
                            }

                            Button(
                                onClick = {
                                    if (zoneName.isNotBlank()) {
                                        // Seed coordinate in center neighborhood
                                        val randomLat = 40.4150 + Math.random() * 0.007
                                        val randomLng = -3.7080 + Math.random() * 0.008
                                        viewModel.addNewSafeZone(
                                            name = zoneName,
                                            type = zoneType,
                                            lat = randomLat,
                                            lng = randomLng,
                                            radiusMeters = radiusMeters.toDouble()
                                        )
                                        // Reset Form
                                        zoneName = ""
                                        zoneType = "Hogar"
                                        radiusMeters = 100f
                                        showAddZoneForm = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("save_zone_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.HighDensityPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Guardar Zona Segura", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                // Safe zones list
                if (safeZones.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay zonas seguras configuradas.", color = com.example.ui.theme.HighDensitySecondaryText)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(safeZones) { zone ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(com.example.ui.theme.HighDensityBg, RoundedCornerShape(14.dp))
                                    .border(1.dp, com.example.ui.theme.HighDensityBorder, RoundedCornerShape(14.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val vector = when (zone.type) {
                                        "Hogar" -> Icons.Outlined.Home
                                        "Escuela" -> Icons.Outlined.School
                                        "Parque" -> Icons.Outlined.Park
                                        else -> Icons.Outlined.PinDrop
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                                            .border(1.dp, com.example.ui.theme.HighDensityBorder, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = vector,
                                            contentDescription = zone.type,
                                            tint = com.example.ui.theme.HighDensityPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = zone.name,
                                            fontWeight = FontWeight.Bold,
                                            color = com.example.ui.theme.HighDensityText,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Radio: ${zone.radiusMeters.toInt()}m | Tipo: ${zone.type}",
                                            color = com.example.ui.theme.HighDensitySecondaryText,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.deleteSafeZone(zone.id) },
                                    modifier = Modifier.size(36.dp).testTag("delete_zone_${zone.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Borrar",
                                        tint = Color(0xFFBA1A1A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SECCIÓN 2: PUNTOS DE INTERÉS PERSONALIZADOS (POI) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.HighDensityBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Puntos de Interés Personalizados (POI)",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFE2725B), // Custom coral for POI
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Marcados específicos creados por los padres",
                            style = MaterialTheme.typography.bodySmall,
                            color = com.example.ui.theme.HighDensitySecondaryText,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(onClick = { showAddPoiForm = !showAddPoiForm }) {
                        Icon(
                            imageVector = if (showAddPoiForm) Icons.Default.Close else Icons.Default.AddCircle,
                            contentDescription = "Agregar POI",
                            tint = Color(0xFFE2725B)
                        )
                    }
                }

                // Collapse Form for Points of Interest
                AnimatedVisibility(visible = showAddPoiForm) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.HighDensityBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.HighDensityBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Añadir Marcador de Interés",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.HighDensityText
                            )

                            OutlinedTextField(
                                value = poiName,
                                onValueChange = { poiName = it },
                                label = { Text("Nombre del Marcador (ej. Escuela, Kárate)", color = com.example.ui.theme.HighDensitySecondaryText) },
                                textStyle = TextStyle(color = com.example.ui.theme.HighDensityText),
                                modifier = Modifier.fillMaxWidth().testTag("poi_name_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFE2725B),
                                    unfocusedBorderColor = com.example.ui.theme.HighDensityBorder
                                ),
                                singleLine = true
                            )

                            // Select POI Type
                            Text(
                                text = "Categoría de Punto:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.HighDensityText
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("Casa", "Escuela", "Actividad Extraescolar", "Otro").forEach { entry ->
                                    val selected = poiType == entry
                                    Button(
                                        onClick = { poiType = entry },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selected) Color(0xFFE2725B) else com.example.ui.theme.HighDensityBg,
                                            contentColor = if (selected) Color.White else com.example.ui.theme.HighDensitySecondaryText
                                        ),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.HighDensityBorder),
                                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (entry == "Actividad Extraescolar") "Extraescolar" else entry,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Sliders for customizable custom Lat/Lng matching local neighborhood madrid centers
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Simular Latitud:", fontSize = 11.sp, color = com.example.ui.theme.HighDensitySecondaryText)
                                    Text(String.format("%.5f", poiLat), fontWeight = FontWeight.Bold, color = Color(0xFFE2725B), fontSize = 11.sp)
                                }
                                Slider(
                                    value = poiLat,
                                    onValueChange = { poiLat = it },
                                    valueRange = 40.4135f..40.4235f,
                                    modifier = Modifier.testTag("poi_latitude_slider")
                                )
                            }

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Simular Longitud:", fontSize = 11.sp, color = com.example.ui.theme.HighDensitySecondaryText)
                                    Text(String.format("%.5f", poiLng), fontWeight = FontWeight.Bold, color = Color(0xFFE2725B), fontSize = 11.sp)
                                }
                                Slider(
                                    value = poiLng,
                                    onValueChange = { poiLng = it },
                                    valueRange = -3.7100f..-3.6980f,
                                    modifier = Modifier.testTag("poi_longitude_slider")
                                )
                            }

                            Button(
                                onClick = {
                                    if (poiName.isNotBlank()) {
                                        viewModel.addPointOfInterest(
                                            name = poiName,
                                            type = poiType,
                                            lat = poiLat.toDouble(),
                                            lng = poiLng.toDouble()
                                        )
                                        // Reset Form
                                        poiName = ""
                                        showAddPoiForm = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("save_poi_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2725B)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Añadir Punto de Interés", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                // POI List
                if (pointsOfInterest.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay puntos personalizados creados.", color = com.example.ui.theme.HighDensitySecondaryText)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pointsOfInterest) { poi ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(com.example.ui.theme.HighDensityBg, RoundedCornerShape(14.dp))
                                    .border(1.dp, com.example.ui.theme.HighDensityBorder, RoundedCornerShape(14.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val iconVector = when (poi.type) {
                                        "Casa" -> Icons.Outlined.Home
                                        "Escuela" -> Icons.Outlined.School
                                        "Actividad Extraescolar" -> Icons.Outlined.SportsGymnastics
                                        else -> Icons.Outlined.Stars
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                                            .border(1.dp, com.example.ui.theme.HighDensityBorder, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = iconVector,
                                            contentDescription = poi.type,
                                            tint = Color(0xFFE2725B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = poi.name,
                                            fontWeight = FontWeight.Bold,
                                            color = com.example.ui.theme.HighDensityText,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "POI: ${poi.type} | Coords: [${String.format("%.4f", poi.latitude)}, ${String.format("%.4f", poi.longitude)}]",
                                            color = com.example.ui.theme.HighDensitySecondaryText,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.deletePointOfInterest(poi.id) },
                                    modifier = Modifier.size(36.dp).testTag("delete_poi_${poi.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Borrar",
                                        tint = Color(0xFFBA1A1A),
                                        modifier = Modifier.size(18.dp)
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

// ----------------------------------------------------
// TAB 2: HISTORIAL DE RUTAS (BREADCRUMBS) Y ALERTAS GEOFENCE
// ----------------------------------------------------
@Composable
fun HistoryAndAlertsPanel(
    viewModel: TrackingViewModel,
    selectedChild: Child?,
    historyPoints: List<RoutePoint>,
    notifications: List<GeofenceNotification>,
    batteryLogs: List<BatteryLog>
) {
    var panelTab by remember { mutableStateOf(0) } // 0: Alert Log, 1: Route History, 2: Battery Log

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Toggle row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { panelTab = 0 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (panelTab == 0) com.example.ui.theme.HighDensityPrimary else MaterialTheme.colorScheme.surface,
                    contentColor = if (panelTab == 0) Color.White else com.example.ui.theme.HighDensitySecondaryText
                ),
                border = if (panelTab == 0) null else androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.HighDensityBorder),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("Registro Alertas", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { panelTab = 1 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (panelTab == 1) com.example.ui.theme.HighDensityPrimary else MaterialTheme.colorScheme.surface,
                    contentColor = if (panelTab == 1) Color.White else com.example.ui.theme.HighDensitySecondaryText
                ),
                border = if (panelTab == 1) null else androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.HighDensityBorder),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("Historial Ruta", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { panelTab = 2 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (panelTab == 2) Color(0xFF006874) else MaterialTheme.colorScheme.surface,
                    contentColor = if (panelTab == 2) Color.White else com.example.ui.theme.HighDensitySecondaryText
                ),
                border = if (panelTab == 2) null else androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.HighDensityBorder),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("Batería Hijo", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (panelTab == 0) {
            // ---- GEOCERCA ALERT LOGS ----
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.HighDensityBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Feeds de Actividad y Geocercas",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.HighDensityText
                        )

                        if (notifications.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearAllNotifications() }) {
                                Text("Limpiar Todo", color = com.example.ui.theme.HighDensityPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (notifications.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No se registran alertas todavía.", color = com.example.ui.theme.HighDensitySecondaryText, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(notifications) { notif ->
                                val cardBg = when (notif.type) {
                                    "DANGER" -> Color(0xFFFFDAD9) // Soft red
                                    "WARNING" -> Color(0xFFFFF1C5) // Soft orange/yellow
                                    "SUCCESS" -> Color(0xFFE8F5E9) // Soft mint green
                                    else -> Color(0xFFF1F3F9) // Soft slate grey
                                }
                                val accentCol = when (notif.type) {
                                    "DANGER" -> Color(0xFFBA1A1A)
                                    "WARNING" -> Color(0xFF825500)
                                    "SUCCESS" -> Color(0xFF386A20)
                                    else -> com.example.ui.theme.HighDensityPrimary
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(cardBg, RoundedCornerShape(12.dp))
                                        .border(1.dp, accentCol.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .padding(top = 4.dp)
                                            .background(accentCol, CircleShape)
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            notif.title,
                                            fontWeight = FontWeight.Bold,
                                            color = accentCol,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            notif.message,
                                            color = com.example.ui.theme.HighDensityText,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp
                                        )
                                    }

                                    val df = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                    Text(
                                        text = df.format(Date(notif.timestamp)),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = com.example.ui.theme.HighDensitySecondaryText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (panelTab == 1) {
            // ---- BREADCRUMBS COORDINATES HISTORY ----
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.HighDensityBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Puntos de Ruta Recorrida: ${selectedChild?.name ?: "Niño"}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.HighDensityText
                        )

                        if (historyPoints.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearHistory() }) {
                                Text("Limpiar Historial", color = Color(0xFFBA1A1A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (historyPoints.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No hay coordenadas de ruta registradas.", color = com.example.ui.theme.HighDensitySecondaryText, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(historyPoints.sortedByDescending { pt -> pt.timestamp }) { pt ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(com.example.ui.theme.HighDensityBg, RoundedCornerShape(12.dp))
                                        .border(1.dp, com.example.ui.theme.HighDensityBorder, RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Lat: ${String.format("%.5f", pt.latitude)} | Lng: ${String.format("%.5f", pt.longitude)}",
                                            color = com.example.ui.theme.HighDensityText,
                                            fontSize = 11.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val timeStr = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date(pt.timestamp))
                                        Text(
                                            text = "Registrado: $timeStr",
                                            color = com.example.ui.theme.HighDensitySecondaryText,
                                            fontSize = 9.sp
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.GpsFixed,
                                        contentDescription = "GPS Pin",
                                        tint = com.example.ui.theme.HighDensityPrimary.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ---- TAB 2: HISTORIAL DEL NIVEL DE BATERÍA ----
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.HighDensityBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Monitoreo de Batería: ${selectedChild?.name ?: "Seleccione un niño"}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF006874)
                    )

                    if (selectedChild == null) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Seleccione un niño en la parte superior para ver su batería.", color = com.example.ui.theme.HighDensitySecondaryText)
                        }
                    } else {
                        // Current level display
                        val level = selectedChild.batteryLevel
                        val threshold = selectedChild.batteryThreshold

                        val batColor = when {
                            level <= threshold -> Color(0xFFBA1A1A) // Urgent low
                            level <= 50 -> Color(0xFF825500) // Medium orange
                            else -> Color(0xFF386A20) // Safe green
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(batColor.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                .border(1.dp, batColor, RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    level <= 15 -> Icons.Outlined.BatteryAlert
                                    level <= 30 -> Icons.Outlined.Battery2Bar
                                    level <= 50 -> Icons.Outlined.Battery4Bar
                                    level <= 80 -> Icons.Outlined.Battery6Bar
                                    else -> Icons.Outlined.BatteryFull
                                },
                                contentDescription = "Batería",
                                tint = batColor,
                                modifier = Modifier.size(36.dp)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Nivel Actual: $level%",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = batColor
                                )
                                Text(
                                    text = if (level <= threshold) "⚠️ ¡Alerta! Dispositivo con batería crítica" else "Estado normal del dispositivo",
                                    fontSize = 11.sp,
                                    color = com.example.ui.theme.HighDensityText,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Set custom threshold slider
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(com.example.ui.theme.HighDensityBg, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .border(1.dp, com.example.ui.theme.HighDensityBorder, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Umbral de Alerta del Padre:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.HighDensityText
                                )
                                Text(
                                    text = "$threshold%",
                                    color = Color(0xFF006874),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                            }

                            Text(
                                text = "El sistema notificará e insertará avisos de panic si la batería del niño cruza este límite.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = com.example.ui.theme.HighDensitySecondaryText
                            )

                            Slider(
                                value = threshold.toFloat(),
                                onValueChange = { newValue ->
                                    viewModel.updateChildThreshold(selectedChild.id, newValue.toInt())
                                },
                                valueRange = 5f..45f,
                                modifier = Modifier.testTag("battery_threshold_slider")
                            )
                        }

                        // Battery logs list
                        Text(
                            text = "Historial de Descarga Registrado:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.HighDensityText,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        if (batteryLogs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No hay registros de descarga todavía.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = com.example.ui.theme.HighDensitySecondaryText
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 140.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(batteryLogs) { log ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(com.example.ui.theme.HighDensityBg, RoundedCornerShape(10.dp))
                                            .border(1.dp, com.example.ui.theme.HighDensityBorder, RoundedCornerShape(10.dp))
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.BatteryChargingFull,
                                                contentDescription = "Récord",
                                                tint = if (log.level <= threshold) Color(0xFFBA1A1A) else Color(0xFF006874),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Batería cruzó a ${log.level}%",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = com.example.ui.theme.HighDensityText
                                            )
                                        }

                                        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                                        Text(
                                            text = "Hora: $timeStr",
                                            fontSize = 10.sp,
                                            color = com.example.ui.theme.HighDensitySecondaryText
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
}

// ----------------------------------------------------
// ROLE SELECTION PANEL (SPLASH SELECTOR)
// ----------------------------------------------------
@Composable
fun SplashScreenSelector(
    onChooseRole: (TrackingViewModel.UserRole) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.ui.theme.HighDensityBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.widthIn(max = 420.dp)
        ) {
            // Elegant Icon / Title header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(com.example.ui.theme.HighDensityPrimary.copy(alpha = 0.15f), CircleShape)
                    .border(2.dp, com.example.ui.theme.HighDensityPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FamilyRestroom,
                    contentDescription = "FamiGuard App Logo",
                    tint = com.example.ui.theme.HighDensityPrimary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "FAMI-GUARD",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = com.example.ui.theme.HighDensityText,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Sistema de Monitoreo & Seguridad de Menores",
                    style = MaterialTheme.typography.bodySmall,
                    color = com.example.ui.theme.HighDensitySecondaryText,
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = "Seleccione cómo inicializar la aplicación en este terminal:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = com.example.ui.theme.HighDensityText,
                modifier = Modifier.padding(top = 16.dp),
                textAlign = TextAlign.Center
            )

            // Selector Button 1: Padre de Familia
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("role_parent_button")
                    .clickable { onChooseRole(TrackingViewModel.UserRole.PADRE) },
                border = androidx.compose.foundation.BorderStroke(2.dp, com.example.ui.theme.HighDensityPrimary)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(com.example.ui.theme.HighDensityPrimary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupervisorAccount,
                            contentDescription = "Padre",
                            tint = com.example.ui.theme.HighDensityPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Iniciar como Padre de Familia",
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.HighDensityText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Monitoree en tiempo real la geolocalización, asigne zonas seguras y administre alertas de pánico y batería de los niños.",
                            color = com.example.ui.theme.HighDensitySecondaryText,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Selector Button 2: Dispositivo Hijo
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("role_child_button")
                    .clickable { onChooseRole(TrackingViewModel.UserRole.NINO) },
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.HighDensityBorder)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFBA1A1A).copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EscalatorWarning,
                            contentDescription = "Hijo (Niño)",
                            tint = Color(0xFFBA1A1A),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Iniciar como Niño / Hijo",
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.HighDensityText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Dispositivo móvil rastreado. Desde aquí el menor puede presionar el Botón de Pánico SOS y reportar el estado de batería.",
                            color = com.example.ui.theme.HighDensitySecondaryText,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "FamiGuard encripta de punto a punto y guarda su información de geofencing en una base de datos local cifrada.",
                style = MaterialTheme.typography.bodySmall,
                color = com.example.ui.theme.HighDensitySecondaryText,
                textAlign = TextAlign.Center,
                fontSize = 10.sp
            )
        }
    }
}

// ----------------------------------------------------
// CHILD DEVICE MODE PANEL
// ----------------------------------------------------
@Composable
fun ChildDeviceScreen(
    viewModel: TrackingViewModel,
    children: List<Child>,
    selectedChildId: String?,
    selectedChild: Child?,
    onBackToSplash: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.ui.theme.HighDensityBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToSplash, modifier = Modifier.testTag("child_back_button")) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Atrás",
                        tint = com.example.ui.theme.HighDensityText
                    )
                }

                Text(
                    text = "Terminal Móvil del Niño",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium,
                    color = com.example.ui.theme.HighDensityText
                )

                IconButton(onClick = { viewModel.selectRole(TrackingViewModel.UserRole.SPLASH) }) {
                    Icon(
                        imageVector = Icons.Default.Cached,
                        contentDescription = "Cambiar Rol",
                        tint = com.example.ui.theme.HighDensityPrimary
                    )
                }
            }

            // Seleccionar cuál dispositivo hijo se está simulando
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.HighDensityBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Seleccione el Niño Activo de este dispositivo:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        color = com.example.ui.theme.HighDensityText
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        children.forEach { child ->
                            val active = child.id == selectedChildId
                            Button(
                                onClick = { viewModel.selectChild(child.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (active) Color(child.avatarColor) else com.example.ui.theme.HighDensityBg,
                                    contentColor = if (active) Color.White else com.example.ui.theme.HighDensitySecondaryText
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = if (active) null else androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.HighDensityBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("select_child_child_mode_${child.id}")
                            ) {
                                Text(child.name, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            if (selectedChild == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cargando perfil del infante...", color = com.example.ui.theme.HighDensitySecondaryText)
                }
            } else {
                // UI de pánico física grande
                val isPanicActive = selectedChild.isPanicActive

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPanicActive) Color(0xFFFCE8E6) else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, if (isPanicActive) Color(0xFFBA1A1A) else com.example.ui.theme.HighDensityBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "BOTÓN DE EMERGENCIA S.O.S.",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFFBA1A1A)
                        )

                        Text(
                            text = if (isPanicActive)
                                "🚨 ¡EL ESTADO DE ALERTA ESTÁ ACTIVO! Un enlace de emergencia ha sido enviado a sus padres familiares con sus coordenadas actuales."
                            else
                                "Mantenga presionado o haga tap para activar una alerta instantánea enviando su geolocalización a todos sus tutores.",
                            style = MaterialTheme.typography.bodySmall,
                            color = com.example.ui.theme.HighDensityText,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = {
                                if (isPanicActive) {
                                    viewModel.resolvePanicAlerts()
                                } else {
                                    viewModel.triggerPanic()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPanicActive) Color.Black else Color(0xFFBA1A1A),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("physical_sos_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPanicActive) Icons.Default.Cancel else Icons.Default.Warning,
                                    contentDescription = "Panic Trigger Button",
                                    tint = Color.White
                                )
                                Text(
                                    text = if (isPanicActive) "DESACTIVAR ALERTA SOS" else "¡ACTIVAR ENVIAR PÁNICO!",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // SIMULAR DRENAJE DE BATERÍA
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.HighDensityBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Simulador del Nivel de Batería",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF006874),
                            style = MaterialTheme.typography.titleSmall
                        )

                        Text(
                            text = "Ajuste el control deslizante de abajo para cambiar la carga de batería simulada del niño. Si baja del umbral (${selectedChild.batteryThreshold}%), se generará una alerta de descarga crítica instantáneamente.",
                            color = com.example.ui.theme.HighDensitySecondaryText,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )

                        val batteryLevel = selectedChild.batteryLevel
                        val threshold = selectedChild.batteryThreshold

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Batería del Niño:",
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.HighDensityText,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "$batteryLevel%",
                                color = if (batteryLevel <= threshold) Color(0xFFBA1A1A) else Color(0xFF006874),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }

                        Slider(
                            value = batteryLevel.toFloat(),
                            onValueChange = { newValue ->
                                viewModel.setChildBattery(selectedChild.id, newValue.toInt())
                            },
                            valueRange = 1f..100f,
                            modifier = Modifier.testTag("child_battery_slider")
                        )

                        if (batteryLevel <= threshold) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFBA1A1A).copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                    .border(1.dp, Color(0xFFBA1A1A), RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BatteryAlert,
                                    contentDescription = "Descarga crítica",
                                    tint = Color(0xFFBA1A1A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Estado: Batería crítica por debajo de $threshold%. Notificación enviada.",
                                    fontSize = 10.sp,
                                    color = Color(0xFFBA1A1A),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = "Estado: Dispositivo seguro. Nivel por encima del umbral de alerta de $threshold%.",
                                color = Color(0xFF386A20),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
