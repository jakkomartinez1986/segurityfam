package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "children")
data class Child(
    @PrimaryKey val id: String,
    val name: String,
    val avatarColor: Int, // Android Color integer representing their theme hex
    val lastLatitude: Double,
    val lastLongitude: Double,
    val lastUpdateTime: Long = System.currentTimeMillis(),
    val isPanicActive: Boolean = false,
    val currentZoneStatus: String = "Fuera de zonas", // e.g., "En Casa", "Fuera de zonas"
    val batteryLevel: Int = 100,
    val batteryThreshold: Int = 20
)

@Entity(tableName = "safe_zones")
data class SafeZone(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val type: String = "Hogar" // "Hogar", "Escuela", "Parque", "Otros"
)

@Entity(tableName = "route_points")
data class RoutePoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "panic_alerts")
data class PanicAlert(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: String,
    val childName: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false
)

@Entity(tableName = "battery_logs")
data class BatteryLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: String,
    val level: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "points_of_interest")
data class PointOfInterest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "Casa", "Escuela", "Actividad Extraescolar", etc.
    val latitude: Double,
    val longitude: Double
)
