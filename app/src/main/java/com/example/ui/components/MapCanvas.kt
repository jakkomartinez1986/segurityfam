package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Child
import com.example.data.RoutePoint
import com.example.data.SafeZone
import com.example.data.PointOfInterest

@Composable
fun MapCanvas(
    children: List<Child>,
    selectedChildId: String?,
    safeZones: List<SafeZone>,
    historyPoints: List<RoutePoint>,
    pointsOfInterest: List<PointOfInterest> = emptyList(),
    modifier: Modifier = Modifier
) {
    // Map bounding boxes (covering the neighborhood simulated in Madrid center)
    val minLat = 40.4135
    val maxLat = 40.4235
    val minLng = -3.7100
    val maxLng = -3.6980

    // Pulse animation for selected child and panic status
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    // Manual Local Zoom Factor
    var zoomFactor by remember { mutableStateOf(1.0f) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(com.example.ui.theme.HighDensityMapBg) // High Density slate canvas background
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            val width = size.width
            val height = size.height

            // Coordinate mapping functions
            fun getX(lng: Double): Float {
                val pct = (lng - minLng) / (maxLng - minLng)
                // Implement relative scaling with zoom factor centered on map midpoint
                val center = width / 2f
                return (pct.toFloat() * width - center) * zoomFactor + center
            }

            fun getY(lat: Double): Float {
                val pct = 1.0 - ((lat - minLat) / (maxLat - minLat))
                val center = height / 2f
                return (pct.toFloat() * height - center) * zoomFactor + center
            }

            // ---- 1. DRAW NEIGHBORHOOD BLOCKS & STREETS ----
            // Drawn as geometric grids to simulate real roads, parks, and blocks
            val gridPaint = com.example.ui.theme.HighDensityBorder
            val streetPaint = com.example.ui.theme.HighDensityStreet

            // Draw illustrative building blocks
            drawRect(
                color = com.example.ui.theme.HighDensityBuildingGreen, // Green zone background (upper-right: Park)
                topLeft = Offset(getX(-3.7025), getY(40.4215)),
                size = Size(200f * zoomFactor, 180f * zoomFactor)
            )

            drawRect(
                color = com.example.ui.theme.HighDensityBuildingCampus, // Campus zone
                topLeft = Offset(getX(-3.7075), getY(40.4225)),
                size = Size(150f * zoomFactor, 150f * zoomFactor)
            )

            // Streets - Horizontal
            drawLine(
                color = streetPaint,
                start = Offset(0f, getY(40.4220)),
                end = Offset(width, getY(40.4220)),
                strokeWidth = 14f * zoomFactor
            )
            drawLine(
                color = streetPaint,
                start = Offset(0f, getY(40.4185)),
                end = Offset(width, getY(40.4185)),
                strokeWidth = 16f * zoomFactor
            )
            drawLine(
                color = streetPaint,
                start = Offset(0f, getY(40.4150)),
                end = Offset(width, getY(40.4150)),
                strokeWidth = 12f * zoomFactor
            )

            // Streets - Vertical
            drawLine(
                color = streetPaint,
                start = Offset(getX(-3.7080), 0f),
                end = Offset(getX(-3.7080), height),
                strokeWidth = 14f * zoomFactor
            )
            drawLine(
                color = streetPaint,
                start = Offset(getX(-3.7030), 0f),
                end = Offset(getX(-3.7030), height),
                strokeWidth = 16f * zoomFactor
            )
            drawLine(
                color = streetPaint,
                start = Offset(getX(-3.7011), 0f),
                end = Offset(getX(-3.7011), height),
                strokeWidth = 14f * zoomFactor
            )

            // Street naming/road guides
            drawCircle(color = gridPaint, radius = 5f, center = Offset(width / 4, height / 4))
            drawCircle(color = gridPaint, radius = 5f, center = Offset(width * 3 / 4, height * 3 / 4))

            // ---- 2. DRAW SAFE ZONES (GEOFENCES) ----
            for (zone in safeZones) {
                val cx = getX(zone.longitude)
                val cy = getY(zone.latitude)
                
                // Radius in pixels. We scale meters proportionally: 
                // Approx 0.001 deg lat is ~111m. Standardize representation pixel radius
                val pxRadius = (zone.radiusMeters.toFloat() * 0.9f) * zoomFactor

                val zoneColor = when (zone.type) {
                    "Hogar" -> com.example.ui.theme.HighDensityPrimary // Azure Blue
                    "Escuela" -> Color(0xFF6750A4) // Premium Purple/Lavender
                    "Parque" -> Color(0xFF386A20) // Deep Botanical Green
                    else -> Color(0xFF74777F) // Secondary Gray
                }

                // Fill area (very subtle translucent)
                drawCircle(
                    color = zoneColor.copy(alpha = 0.15f),
                    radius = pxRadius,
                    center = Offset(cx, cy)
                )

                // Outer border (dashed styles for boundaries)
                drawCircle(
                    color = zoneColor.copy(alpha = 0.80f),
                    radius = pxRadius,
                    center = Offset(cx, cy),
                    style = Stroke(
                        width = 3.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                    )
                )

                // Draw central icon anchor
                drawCircle(
                    color = zoneColor,
                    radius = 6f * zoomFactor,
                    center = Offset(cx, cy)
                )
            }

            // ---- 2b. DRAW POINTS OF INTEREST (CUSTOM MARKERS) ----
            for (poi in pointsOfInterest) {
                val cx = getX(poi.longitude)
                val cy = getY(poi.latitude)
                // draw coral pins/symbols for custom POIs
                drawCircle(
                    color = Color(0xFFE2725B), // Coral Accent
                    radius = 8f * zoomFactor,
                    center = Offset(cx, cy)
                )
                // Draw a nice white outline
                drawCircle(
                    color = Color.White,
                    radius = 8f * zoomFactor,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5f * zoomFactor)
                )
            }

            // ---- 3. DRAW HISTORICAL ROUTE PATH TRAIL ----
            if (historyPoints.isNotEmpty()) {
                val activeChild = children.find { it.id == selectedChildId }
                val trailColor = if (activeChild != null) Color(activeChild.avatarColor) else Color.White

                val path = Path()
                val sortedPoints = historyPoints.sortedBy { it.timestamp }
                if (sortedPoints.isNotEmpty()) {
                    path.moveTo(
                        getX(sortedPoints.first().longitude),
                        getY(sortedPoints.first().latitude)
                    )
                    for (i in 1 until sortedPoints.size) {
                        path.lineTo(
                            getX(sortedPoints[i].longitude),
                            getY(sortedPoints[i].latitude)
                        )
                    }

                    // Draw historical breadcrumbs trail
                    drawPath(
                        path = path,
                        color = trailColor.copy(alpha = 0.85f),
                        style = Stroke(
                            width = 6f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 12f), 0f)
                        )
                    )

                    // Draw dots for historic location updates
                    for (pt in sortedPoints) {
                        drawCircle(
                            color = trailColor,
                            radius = 4.5f,
                            center = Offset(getX(pt.longitude), getY(pt.latitude))
                        )
                    }
                }
            }

            // ---- 4. DRAW CHILD MARKERS ----
            for (child in children) {
                val cx = getX(child.lastLongitude)
                val cy = getY(child.lastLatitude)
                val markerColor = Color(child.avatarColor)

                // Select/Focus indicator or active Panic warning pulse
                if (child.isPanicActive) {
                    // Panic Alert: Huge pulsating red aura
                    drawCircle(
                        color = Color.Red.copy(alpha = pulseAlpha),
                        radius = (pulseRadius * 2.2f) * zoomFactor,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = Color.Red.copy(alpha = 0.4f),
                        radius = 18f * zoomFactor,
                        center = Offset(cx, cy)
                    )
                } else if (child.id == selectedChildId) {
                    // Highlight selected child with general pulse
                    drawCircle(
                        color = markerColor.copy(alpha = pulseAlpha),
                        radius = (pulseRadius * 1.5f) * zoomFactor,
                        center = Offset(cx, cy)
                    )
                }

                // Core Pin Marker
                drawCircle(
                    color = Color.White,
                    radius = 15f * zoomFactor,
                    center = Offset(cx, cy),
                    style = Stroke(width = 3.5f)
                )

                drawCircle(
                    color = markerColor,
                    radius = 11.5f * zoomFactor,
                    center = Offset(cx, cy)
                )

                // Draw central initial tag string "S" or "M"
                // Using DrawScope native letters
            }
        }

        // ---- Zoom CONTROLLERS (Floating overlays) ----
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = { zoomFactor = (zoomFactor + 0.2f).coerceAtMost(3.0f) },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Aumentar Zoom",
                    modifier = Modifier.size(22.dp)
                )
            }

            FloatingActionButton(
                onClick = { zoomFactor = (zoomFactor - 0.2f).coerceAtLeast(0.5f) },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Disminuir Zoom",
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Inner Legend labels
        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, com.example.ui.theme.HighDensityBorder)
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Mapa Vecindario",
                    style = MaterialTheme.typography.labelSmall,
                    color = com.example.ui.theme.HighDensityText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).background(com.example.ui.theme.HighDensityPrimary, CircleShape))
                    Text("Casa", style = MaterialTheme.typography.labelSmall, color = com.example.ui.theme.HighDensitySecondaryText, fontSize = 9.sp)
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF6750A4), CircleShape))
                    Text("Escuela", style = MaterialTheme.typography.labelSmall, color = com.example.ui.theme.HighDensitySecondaryText, fontSize = 9.sp)
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF386A20), CircleShape))
                    Text("Parque", style = MaterialTheme.typography.labelSmall, color = com.example.ui.theme.HighDensitySecondaryText, fontSize = 9.sp)
                }
            }
        }
    }
}
