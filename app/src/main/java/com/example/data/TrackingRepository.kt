package com.example.data

import kotlinx.coroutines.flow.Flow

class TrackingRepository(private val dao: TrackingDao) {

    val children: Flow<List<Child>> = dao.getAllChildren()
    val safeZones: Flow<List<SafeZone>> = dao.getAllSafeZones()
    val panicAlerts: Flow<List<PanicAlert>> = dao.getAllPanicAlerts()

    fun getChildById(id: String): Flow<Child?> = dao.getChildById(id)
    
    fun getRoutePointsForChild(childId: String): Flow<List<RoutePoint>> = 
        dao.getRoutePointsForChild(childId)

    suspend fun insertChild(child: Child) = dao.insertChild(child)
    
    suspend fun updateChild(child: Child) = dao.updateChild(child)

    suspend fun insertSafeZone(zone: SafeZone) = dao.insertSafeZone(zone)
    
    suspend fun deleteSafeZoneById(id: Long) = dao.deleteSafeZoneById(id)

    suspend fun insertRoutePoint(point: RoutePoint) = dao.insertRoutePoint(point)
    
    suspend fun clearHistoryForChild(childId: String) = dao.clearHistoryForChild(childId)

    suspend fun insertPanicAlert(alert: PanicAlert) = dao.insertPanicAlert(alert)
    
    suspend fun resolvePanicAlert(id: Long) = dao.resolvePanicAlert(id)
    
    suspend fun clearAllPanicAlerts() = dao.clearAllPanicAlerts()

    // Battery logs
    fun getAllBatteryLogsForChild(childId: String): Flow<List<BatteryLog>> =
        dao.getAllBatteryLogsForChild(childId)

    suspend fun insertBatteryLog(log: BatteryLog) = dao.insertBatteryLog(log)

    // Points of interest
    val pointsOfInterest: Flow<List<PointOfInterest>> = dao.getAllPointsOfInterest()

    suspend fun insertPointOfInterest(poi: PointOfInterest) = dao.insertPointOfInterest(poi)

    suspend fun deletePointOfInterestById(id: Long) = dao.deletePointOfInterestById(id)
}
