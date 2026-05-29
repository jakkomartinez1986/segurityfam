package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackingDao {
    // Children CRUD
    @Query("SELECT * FROM children")
    fun getAllChildren(): Flow<List<Child>>

    @Query("SELECT * FROM children WHERE id = :id")
    fun getChildById(id: String): Flow<Child?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChild(child: Child)

    @Update
    suspend fun updateChild(child: Child)

    // Safe Zones (Geofencing) CRUD
    @Query("SELECT * FROM safe_zones")
    fun getAllSafeZones(): Flow<List<SafeZone>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSafeZone(zone: SafeZone)

    @Query("DELETE FROM safe_zones WHERE id = :id")
    suspend fun deleteSafeZoneById(id: Long)

    // Route points history
    @Query("SELECT * FROM route_points WHERE childId = :childId ORDER BY timestamp ASC")
    fun getRoutePointsForChild(childId: String): Flow<List<RoutePoint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutePoint(point: RoutePoint)

    @Query("DELETE FROM route_points WHERE childId = :childId")
    suspend fun clearHistoryForChild(childId: String)

    // Panic Alerts Log
    @Query("SELECT * FROM panic_alerts ORDER BY timestamp DESC")
    fun getAllPanicAlerts(): Flow<List<PanicAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPanicAlert(alert: PanicAlert)

    @Query("UPDATE panic_alerts SET isResolved = 1 WHERE id = :id")
    suspend fun resolvePanicAlert(id: Long)

    @Query("DELETE FROM panic_alerts")
    suspend fun clearAllPanicAlerts()

    // Battery Logs CRUD
    @Query("SELECT * FROM battery_logs WHERE childId = :childId ORDER BY timestamp DESC")
    fun getAllBatteryLogsForChild(childId: String): Flow<List<BatteryLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatteryLog(log: BatteryLog)

    // Points of Interest CRUD
    @Query("SELECT * FROM points_of_interest ORDER BY id ASC")
    fun getAllPointsOfInterest(): Flow<List<PointOfInterest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPointOfInterest(poi: PointOfInterest)

    @Query("DELETE FROM points_of_interest WHERE id = :id")
    suspend fun deletePointOfInterestById(id: Long)
}

@Database(entities = [Child::class, SafeZone::class, RoutePoint::class, PanicAlert::class, BatteryLog::class, PointOfInterest::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract val trackingDao: TrackingDao
}
