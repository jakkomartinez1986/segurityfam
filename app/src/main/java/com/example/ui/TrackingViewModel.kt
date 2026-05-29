package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.*

class TrackingViewModel(private val repository: TrackingRepository) : ViewModel() {

    // Active state flows from DB
    val children: StateFlow<List<Child>> = repository.children
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val safeZones: StateFlow<List<SafeZone>> = repository.safeZones
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val panicAlerts: StateFlow<List<PanicAlert>> = repository.panicAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Local State
    private val _selectedChildId = MutableStateFlow<String?>("sofia")
    val selectedChildId: StateFlow<String?> = _selectedChildId.asStateFlow()

    enum class UserRole {
        SPLASH, PADRE, NINO
    }

    private val _currentUserRole = MutableStateFlow(UserRole.SPLASH)
    val currentUserRole: StateFlow<UserRole> = _currentUserRole.asStateFlow()

    fun selectRole(role: UserRole) {
        _currentUserRole.value = role
    }

    private val _isSimulationRunning = MutableStateFlow(false)
    val isSimulationRunning: StateFlow<Boolean> = _isSimulationRunning.asStateFlow()

    // Real-time local banners/notifications of geofencing and panic events
    private val _notifications = MutableStateFlow<List<GeofenceNotification>>(emptyList())
    val notifications: StateFlow<List<GeofenceNotification>> = _notifications.asStateFlow()

    // Route points for currently selected child
    val selectedChildHistory: StateFlow<List<RoutePoint>> = _selectedChildId
        .flatMapLatest { childId ->
            if (childId != null) {
                repository.getRoutePointsForChild(childId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pointsOfInterest: StateFlow<List<PointOfInterest>> = repository.pointsOfInterest
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedChildBatteryLogs: StateFlow<List<BatteryLog>> = _selectedChildId
        .flatMapLatest { childId ->
            if (childId != null) {
                repository.getAllBatteryLogsForChild(childId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Simulation indices and pathways
    private val pathIndices = java.util.concurrent.ConcurrentHashMap<String, Int>().apply {
        put("sofia", 0)
        put("mateo", 0)
    }

    private val sofiaPath = listOf(
        Pair(40.4175, -3.7080), // Casa Sofía
        Pair(40.4182, -3.7072),
        Pair(40.4189, -3.7061),
        Pair(40.4196, -3.7051),
        Pair(40.4205, -3.7042),
        Pair(40.4220, -3.7030), // Colegio Primaria
        Pair(40.4215, -3.7018),
        Pair(40.4202, -3.7011),
        Pair(40.4190, -3.7011), // Parque Central
        Pair(40.4181, -3.7025),
        Pair(40.4173, -3.7048),
        Pair(40.4175, -3.7080)  // Volver a Casa
    )

    private val mateoPath = listOf(
        Pair(40.4150, -3.7020), // Casa Mateo
        Pair(40.4158, -3.7025),
        Pair(40.4167, -3.7032),
        Pair(40.4176, -3.7041), // Biblioteca / Polideportivo
        Pair(40.4182, -3.7048),
        Pair(40.4190, -3.7052),
        Pair(40.4195, -3.7035),
        Pair(40.4190, -3.7011), // Parque Central
        Pair(40.4178, -3.7005),
        Pair(40.4162, -3.7011),
        Pair(40.4150, -3.7020)  // Volver a Casa
    )

    private var simulationJob: Job? = null

    init {
        // Enforce DB seed data for a rich, ready-to-test demonstration
        viewModelScope.launch {
            repository.safeZones.first().let { zones ->
                if (zones.isEmpty()) {
                    seedDatabase()
                }
            }
        }
    }

    private suspend fun seedDatabase() {
        // Safe Zones
        repository.insertSafeZone(SafeZone(1, "Casa de Sofía", 40.4175, -3.7080, 100.0, "Hogar"))
        repository.insertSafeZone(SafeZone(2, "Colegio Primaria", 40.4220, -3.7030, 150.0, "Escuela"))
        repository.insertSafeZone(SafeZone(3, "Parque de Sol", 40.4190, -3.7011, 180.0, "Parque"))
        repository.insertSafeZone(SafeZone(4, "Casa de Mateo", 40.4150, -3.7020, 80.0, "Hogar"))

        // Children Initial Position
        repository.insertChild(Child("sofia", "Sofía", 0xFF2A9D8F.toInt(), 40.4175, -3.7080, currentZoneStatus = "En Casa (Casa de Sofía)"))
        repository.insertChild(Child("mateo", "Mateo", 0xFFF4A261.toInt(), 40.4150, -3.7020, currentZoneStatus = "En Casa (Casa de Mateo)"))

        // Initial Route points
        repository.insertRoutePoint(RoutePoint(childId = "sofia", latitude = 40.4175, longitude = -3.7080))
        repository.insertRoutePoint(RoutePoint(childId = "mateo", latitude = 40.4150, longitude = -3.7020))
    }

    fun selectChild(childId: String) {
        viewModelScope.launch {
            _selectedChildId.value = childId
        }
    }

    fun toggleSimulation() {
        if (_isSimulationRunning.value) {
            _isSimulationRunning.value = false
            simulationJob?.cancel()
        } else {
            _isSimulationRunning.value = true
            startSimulationLoop()
        }
    }

    private fun startSimulationLoop() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            while (_isSimulationRunning.value) {
                delay(3000) // update location every 3 seconds
                updateSimulatedLocations()
            }
        }
    }

    private suspend fun updateSimulatedLocations() {
        val currentChildren = children.value
        val zones = safeZones.value

        for (child in currentChildren) {
            // Pick next path point for each child
            val path = if (child.id == "sofia") sofiaPath else mateoPath
            val currentIndex = pathIndices[child.id] ?: 0
            val nextIndex = (currentIndex + 1) % path.size
            pathIndices[child.id] = nextIndex

            val (lat, lng) = path[nextIndex]
            processChildLocationUpdate(child, lat, lng, zones)
        }
    }

    fun manuallyStepSimulation() {
        viewModelScope.launch {
            val zones = safeZones.value
            val currentChildren = children.value
            val activeId = _selectedChildId.value ?: return@launch
            val child = currentChildren.find { it.id == activeId } ?: return@launch

            val path = if (child.id == "sofia") sofiaPath else mateoPath
            val currentIndex = pathIndices[child.id] ?: 0
            val nextIndex = (currentIndex + 1) % path.size
            pathIndices[child.id] = nextIndex

            val (lat, lng) = path[nextIndex]
            processChildLocationUpdate(child, lat, lng, zones)
        }
    }

    fun teleportChild(childId: String, outOfZones: Boolean) {
        viewModelScope.launch {
            val zones = safeZones.value
            val currentChildren = children.value
            val child = currentChildren.find { it.id == childId } ?: return@launch

            val (lat, lng) = if (outOfZones) {
                // Out of any zone
                Pair(40.4250, -3.7120)
            } else {
                // Teleport to "Colegio Primaria" centered at 40.4220, -3.7030
                Pair(40.4219, -3.7028)
            }
            processChildLocationUpdate(child, lat, lng, zones)
        }
    }

    private suspend fun processChildLocationUpdate(
        child: Child,
        lat: Double,
        lng: Double,
        zones: List<SafeZone>
    ) {
        // Determine closest safe zone
        var insideZone: SafeZone? = null
        for (zone in zones) {
            val dist = calculateDistanceInMeters(lat, lng, zone.latitude, zone.longitude)
            if (dist <= zone.radiusMeters) {
                insideZone = zone
                break
            }
        }

        val previousStatus = child.currentZoneStatus
        val nextStatus = if (insideZone != null) {
            "En ${insideZone.type} (${insideZone.name})"
        } else {
            "Fuera de zonas"
        }

        // Detect Zone Transition
        if (previousStatus != nextStatus) {
            val transitionType = when {
                previousStatus.contains("Fuera de zonas") && insideZone != null -> "ENTRY"
                !previousStatus.contains("Fuera de zonas") && insideZone == null -> "EXIT"
                insideZone != null -> "TRANSFER"
                else -> ""
            }

            if (transitionType.isNotEmpty()) {
                val detail = if (transitionType == "ENTRY") {
                    "${child.name} ingresó a la zona segura: ${insideZone?.name}"
                } else if (transitionType == "EXIT") {
                    "${child.name} salió de su zona segura"
                } else {
                    "${child.name} se trasladó a ${insideZone?.name}"
                }

                addLocalNotification(
                    title = "Alerta de Zona Segura",
                    message = detail,
                    childId = child.id,
                    type = if (transitionType == "EXIT") "WARNING" else "SUCCESS"
                )
            }
        }

        // Save updated child coordinates and state
        val updatedChild = child.copy(
            lastLatitude = lat,
            lastLongitude = lng,
            lastUpdateTime = System.currentTimeMillis(),
            currentZoneStatus = nextStatus
        )
        repository.updateChild(updatedChild)

        // Save historical route trail breadcrumb
        repository.insertRoutePoint(
            RoutePoint(
                childId = child.id,
                latitude = lat,
                longitude = lng,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun triggerPanic() {
        viewModelScope.launch {
            val activeId = _selectedChildId.value ?: return@launch
            val currentChildren = children.value
            val child = currentChildren.find { it.id == activeId } ?: return@launch

            val updatedChild = child.copy(isPanicActive = true)
            repository.updateChild(updatedChild)

            val alert = PanicAlert(
                childId = child.id,
                childName = child.name,
                latitude = child.lastLatitude,
                longitude = child.lastLongitude,
                timestamp = System.currentTimeMillis()
            )
            repository.insertPanicAlert(alert)

            addLocalNotification(
                title = "🚨 BOTÓN DE PÁNICO 🚨",
                message = "¡ALERTA DE PÁNICO activada por ${child.name}! Envío inmediato de ubicación.",
                childId = child.id,
                type = "DANGER"
            )
        }
    }

    fun resolvePanic(alertId: Long, childId: String) {
        viewModelScope.launch {
            repository.resolvePanicAlert(alertId)
            
            // Turn off child panic flag
            val currentChildren = children.value
            val child = currentChildren.find { it.id == childId }
            if (child != null) {
                repository.updateChild(child.copy(isPanicActive = false))
            }

            addLocalNotification(
                title = "Alerta Resuelta",
                message = "La alerta de pánico de ${child?.name ?: "niño"} ha sido resuelta.",
                childId = childId,
                type = "INFO"
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            val activeId = _selectedChildId.value ?: return@launch
            repository.clearHistoryForChild(activeId)
        }
    }

    fun addNewSafeZone(name: String, type: String, lat: Double, lng: Double, radiusMeters: Double) {
        viewModelScope.launch {
            val zone = SafeZone(
                name = name,
                type = type,
                latitude = lat,
                longitude = lng,
                radiusMeters = radiusMeters
            )
            repository.insertSafeZone(zone)
            addLocalNotification(
                title = "Zona Creada",
                message = "Nueva zona '$name' guardada con éxito.",
                childId = "",
                type = "INFO"
            )
        }
    }

    fun deleteSafeZone(zoneId: Long) {
        viewModelScope.launch {
            repository.deleteSafeZoneById(zoneId)
        }
    }

    // Points of Interest API
    fun addPointOfInterest(name: String, type: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            val poi = PointOfInterest(name = name, type = type, latitude = lat, longitude = lng)
            repository.insertPointOfInterest(poi)
            addLocalNotification(
                title = "POI Añadido",
                message = "Se añadió el punto de interés '$name' ($type).",
                childId = "",
                type = "SUCCESS"
            )
        }
    }

    fun deletePointOfInterest(id: Long) {
        viewModelScope.launch {
            repository.deletePointOfInterestById(id)
        }
    }

    // Battery alert levels API
    fun updateChildThreshold(childId: String, threshold: Int) {
        viewModelScope.launch {
            val child = children.value.find { it.id == childId } ?: return@launch
            repository.updateChild(child.copy(batteryThreshold = threshold))
        }
    }

    fun setChildBattery(childId: String, level: Int) {
        viewModelScope.launch {
            val child = children.value.find { it.id == childId } ?: return@launch
            val wasBelowThreshold = child.batteryLevel <= child.batteryThreshold
            val isNowBelowThreshold = level <= child.batteryThreshold

            repository.updateChild(child.copy(batteryLevel = level))
            repository.insertBatteryLog(BatteryLog(childId = childId, level = level))

            if (!wasBelowThreshold && isNowBelowThreshold) {
                // Insert auto notification & Panic Alert
                addLocalNotification(
                    title = "🔋 BATERÍA CRÍTICA: ${child.name}",
                    message = "El dispositivo de ${child.name} tiene un $level% de carga (Umbral: ${child.batteryThreshold}%).",
                    childId = childId,
                    type = "DANGER"
                )

                // Trigger emergency warning SOS if extremely low or cross check threshold
                val alert = PanicAlert(
                    childId = child.id,
                    childName = child.name,
                    latitude = child.lastLatitude,
                    longitude = child.lastLongitude,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertPanicAlert(alert)
                repository.updateChild(child.copy(batteryLevel = level, isPanicActive = true))
            } else if (wasBelowThreshold && !isNowBelowThreshold) {
                addLocalNotification(
                    title = "🔋 Batería Recuperada: ${child.name}",
                    message = "El dispositivo de ${child.name} ha sido cargado a $level%.",
                    childId = childId,
                    type = "SUCCESS"
                )
            }
        }
    }

    fun resolvePanicAlerts() {
        viewModelScope.launch {
            val activeId = _selectedChildId.value ?: return@launch
            val activeChild = children.value.find { it.id == activeId } ?: return@launch
            repository.updateChild(activeChild.copy(isPanicActive = false))
            
            // Resolve any open panic alerts for this child in DB
            val alerts = panicAlerts.value.filter { it.childId == activeId && !it.isResolved }
            alerts.forEach { alert ->
                repository.resolvePanicAlert(alert.id)
            }

            addLocalNotification(
                title = "Alertas SOS Resueltas",
                message = "Se han cancelado las alertas de SOS de ${activeChild.name}.",
                childId = activeId,
                type = "INFO"
            )
        }
    }

    fun clearAllNotifications() {
        _notifications.value = emptyList()
    }

    private fun addLocalNotification(title: String, message: String, childId: String, type: String) {
        val newNotification = GeofenceNotification(
            id = System.currentTimeMillis(),
            title = title,
            message = message,
            childId = childId,
            type = type,
            timestamp = System.currentTimeMillis()
        )
        _notifications.value = listOf(newNotification) + _notifications.value
    }

    // Mathematical utility: Haversine distance
    private fun calculateDistanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val latRad1 = Math.toRadians(lat1)
        val latRad2 = Math.toRadians(lat2)

        val a = sin(dLat / 2).pow(2) + cos(latRad1) * cos(latRad2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}

// Model wrapper for live geofencing alert notifications
data class GeofenceNotification(
    val id: Long,
    val title: String,
    val message: String,
    val childId: String,
    val type: String, // "SUCCESS", "WARNING", "DANGER", "INFO"
    val timestamp: Long
)

class TrackingViewModelFactory(private val repository: TrackingRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrackingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TrackingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
