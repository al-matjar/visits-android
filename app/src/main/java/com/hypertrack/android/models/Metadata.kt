package com.hypertrack.android.models

import com.hypertrack.android.utils.CrashReportsProvider
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import org.jetbrains.annotations.TestOnly
import java.lang.ClassCastException

@JsonClass(generateAdapter = true)
data class Metadata(
    val otherMetadata: Map<String, String>,
    var visitsAppMetadata: VisitsAppMetadata
) {

    @TestOnly
    fun toMap(): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            otherMetadata.forEach { (k, v) -> put(k, v) }
            put(VISITS_APP_KEY, visitsAppMetadata.toMap())
        }
    }

    companion object {
        const val VISITS_APP_KEY = "visits_app"
        private const val HT_KEY = "ht_"

        fun empty() = Metadata(mapOf(), VisitsAppMetadata())

        @Suppress("UNCHECKED_CAST")
        fun deserialize(map: Map<String, Any>): Metadata {
            return Metadata(
                visitsAppMetadata = try {
                    VisitsAppMetadata.deserialize(map[VISITS_APP_KEY] as Map<String, Any>)
                } catch (_: Exception) {
                    VisitsAppMetadata()
                },
                otherMetadata = map
                    .filterKeys {
                        it != VISITS_APP_KEY && !it.startsWith(HT_KEY)
                    }
                    .filter { it.value is String } as Map<String, String>
            )
        }
    }

}

@JsonClass(generateAdapter = true)
data class VisitsAppMetadata(
    val appended: Map<String, AppendMetadata>? = null,
    var note: String? = null,
    var photos: List<String>? = null,
    @field:Json(name = "user_location") val userLocation: LocationData? = null,
) {

    fun addPhoto(photoId: String) {
        photos = (photos ?: listOf()).toMutableList().apply {
            add(photoId)
        }.toSet().toList()
    }

    @TestOnly
    fun toMap(): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            appended?.forEach { k, v -> put(k, v.toMap()) }
            note?.let { put(KEY_NOTE, it as Any) }
            photos?.let { put(KEY_PHOTOS, it as Any) }
            userLocation?.let { put(KEY_USER_LOCATION, it as Any) }
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun deserialize(map: Map<String, Any>?): VisitsAppMetadata {
            if (map == null) return VisitsAppMetadata()
            return VisitsAppMetadata(
                note = try {
                    map["note"] as String
                } catch (_: Exception) {
                    null
                },
                photos = try {
                    map["photos"] as List<String>
                } catch (_: Exception) {
                    null
                },
                appended = try {
                    (map["appended"] as Map<String, Any>).mapValues {
                        AppendMetadata.deserialize(it.value as Map<String, Any>)
                    }
                } catch (_: Exception) {
                    null
                },
                userLocation = null
            )
        }
    }
}

@JsonClass(generateAdapter = true)
data class AppendMetadata(
    val note: String? = null,
    val photos: List<String>? = null,
    @field:Json(name = "user_location") val userLocation: LocationData? = null
) {

    @TestOnly
    fun toMap(): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            note?.let { put(KEY_NOTE, it as Any) }
            photos?.let { put(KEY_PHOTOS, it as Any) }
            userLocation?.let { put(KEY_USER_LOCATION, it as Any) }
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun deserialize(map: Map<String, Any>?): AppendMetadata {
            if (map == null) return AppendMetadata()
            return AppendMetadata(
                note = try {
                    map["note"] as String
                } catch (_: Exception) {
                    null
                },
                photos = try {
                    map["photos"] as List<String>
                } catch (_: Exception) {
                    null
                },
                userLocation = null
            )
        }
    }

}

@JsonClass(generateAdapter = true)
class GeofenceMetadata(
    val integration: Integration? = null,
    val address: String? = null,
    val name: String? = null,
    val description: String? = null,
) {
    fun toMap(moshi: Moshi): Map<String, Any> {
        return moshi.adapter(GeofenceMetadata::class.java)
            .toJsonValue(this) as Map<String, Any>
    }

    companion object {
        const val KEY_ADDRESS = "address"
        const val KEY_NAME = "name"
        const val KEY_INTEGRATION = "integration"
        const val KEY_DESCRIPTION = "description"

        //todo refactor
        fun fromMap(
            map: Map<String, Any>,
            crashReportsProvider: CrashReportsProvider
        ): GeofenceMetadata {

            fun handleException(e: Exception) {
                if (e !is ClassCastException) {
                    crashReportsProvider.logException(e)
                }
            }

            //todo fix integration (should be parsed from map)
            return GeofenceMetadata(
                integration = null,
//                integration = try {
//                    map[KEY_INTEGRATION] as Integration?
//                } catch (e: Exception) {
//                    handleException(e)
//                    null
//                },
                name = try {
                    map[KEY_NAME] as String?
                } catch (e: Exception) {
                    handleException(e)
                    null
                },
                address = try {
                    map[KEY_ADDRESS] as String?
                } catch (e: Exception) {
                    handleException(e)
                    null
                },
                description = try {
                    map[KEY_DESCRIPTION] as String?
                } catch (e: Exception) {
                    handleException(e)
                    null
                },
            )
        }
    }
}

private const val KEY_NOTE = "note"
private const val KEY_PHOTOS = "photos"
private const val KEY_USER_LOCATION = "user_location"

