package com.hypertrack.android.repository

import com.fonfon.kgeohash.GeoHash
import com.hypertrack.android.api.ApiClient
import com.hypertrack.android.api.GeofenceVisit
import com.hypertrack.android.interactors.GeofenceError
import com.hypertrack.android.interactors.GeofenceResult
import com.hypertrack.android.interactors.GeofenceSuccess
import com.hypertrack.android.models.GeofenceMetadata
import com.hypertrack.android.models.Integration
import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.models.local.LocalGeofenceVisit
import com.hypertrack.android.ui.common.DataPage
import com.hypertrack.android.ui.common.util.nullIfBlank
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.OsUtilsProvider
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

interface PlacesRepository {
    suspend fun loadGeofencesPage(pageToken: String?): DataPage<LocalGeofence>
    suspend fun loadGeofencesPageForMap(gh: GeoHash, pageToken: String?): DataPage<LocalGeofence>

    suspend fun createGeofence(
        latitude: Double,
        longitude: Double,
        radius: Int,
        name: String? = null,
        address: String? = null,
        description: String? = null,
        integration: Integration? = null
    ): CreateGeofenceResult

    suspend fun getGeofence(geofenceId: String): GeofenceResult
}

class PlacesRepositoryImpl(
    private val deviceId: DeviceId,
    private val apiClient: ApiClient,
    private val moshi: Moshi,
    private val osUtilsProvider: OsUtilsProvider,
    private val crashReportsProvider: CrashReportsProvider
) : PlacesRepository {

    override suspend fun loadGeofencesPage(pageToken: String?): DataPage<LocalGeofence> {
        return loadGeofences(pageToken, null)
    }

    override suspend fun loadGeofencesPageForMap(
        gh: GeoHash,
        pageToken: String?
    ): DataPage<LocalGeofence> {
        return loadGeofences(pageToken, gh)
    }

    private suspend fun loadGeofences(pageToken: String?, gh: GeoHash?): DataPage<LocalGeofence> {
        return withContext(Dispatchers.IO) {
            val res = apiClient.getGeofences(pageToken, gh.string())
            val localGeofences =
                res.geofences.map {
                    LocalGeofence.fromGeofence(
                        deviceId,
                        it,
                        moshi,
                        osUtilsProvider,
                        crashReportsProvider
                    )
                }
            DataPage(
                localGeofences,
                res.paginationToken
            )
        }
    }

    override suspend fun createGeofence(
        latitude: Double,
        longitude: Double,
        radius: Int,
        name: String?,
        address: String?,
        description: String?,
        integration: Integration?
    ): CreateGeofenceResult {
        try {
            val res = apiClient.createGeofence(
                latitude = latitude,
                longitude = longitude,
                radius = radius,
                GeofenceMetadata(
                    name = name.nullIfBlank() ?: integration?.name,
                    integration = integration,
                    description = description.nullIfBlank(),
                    address = address.nullIfBlank()
                )
            )
            if (res.isSuccessful) {
                return CreateGeofenceSuccess(
                    LocalGeofence.fromGeofence(
                        deviceId,
                        res.body()!!.first(),
                        moshi,
                        osUtilsProvider,
                        crashReportsProvider
                    )
                )
            } else {
                return CreateGeofenceError(HttpException(res))
            }
        } catch (e: Exception) {
            return CreateGeofenceError(e)
        }
    }

    override suspend fun getGeofence(geofenceId: String): GeofenceResult {
        return try {
            apiClient.getGeofence(geofenceId).let {
                LocalGeofence.fromGeofence(
                    deviceId,
                    it,
                    moshi,
                    osUtilsProvider,
                    crashReportsProvider
                )
            }.let {
                GeofenceSuccess(it)
            }
        } catch (e: Exception) {
            GeofenceError(e)
        }
    }
}

fun GeoHash?.string() = this?.let { it.toString() }

sealed class CreateGeofenceResult
class CreateGeofenceSuccess(val geofence: LocalGeofence) : CreateGeofenceResult()
class CreateGeofenceError(val e: Exception) : CreateGeofenceResult()
