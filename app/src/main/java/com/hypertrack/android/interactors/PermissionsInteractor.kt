package com.hypertrack.android.interactors

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.hypertrack.android.utils.HyperTrackService
import com.hypertrack.android.utils.MyApplication
import javax.inject.Provider

interface PermissionsInteractor {
    fun checkPermissionsState(): PermissionsState
    fun requestRequiredPermissions(activity: Activity)
    fun requestBackgroundLocationPermission(activity: Activity)
    fun isBackgroundLocationGranted(): Boolean
    fun isBasePermissionsGranted(): Boolean
    fun isAllPermissionsGranted(): Boolean
}

class PermissionsInteractorImpl(
    private val hyperTrackProvider: Provider<HyperTrackService>
) : PermissionsInteractor {


    override fun checkPermissionsState(): PermissionsState {
        return PermissionsState(
            activityTrackingGranted = isActivityGranted(),
            foregroundLocationGranted = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION),
            backgroundLocationGranted = isBackgroundLocationGranted(),
        )
    }

    override fun isBasePermissionsGranted(): Boolean {
        return isActivityGranted() && hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    override fun isAllPermissionsGranted(): Boolean {
        return isBasePermissionsGranted() && isBackgroundLocationGranted()
    }

    private fun isActivityGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            true
        }
    }

    override fun isBackgroundLocationGranted(): Boolean {
        // we don't need ACCESS_BACKGROUND_LOCATION before R see
        // https://hypertrack.com/docs/install-sdk-android/#what-permissions-are-required-for-the-sdk-to-work
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            true
        }
    }

    override fun requestRequiredPermissions(activity: Activity) {
        hyperTrackProvider.get().showPermissionsPrompt()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun requestBackgroundLocationPermission(activity: Activity) {
        activity.requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 42)
    }

    private fun hasPermission(permission: String) =
        MyApplication.context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED


}

class PermissionsState(
    val activityTrackingGranted: Boolean,
    val foregroundLocationGranted: Boolean,
    val backgroundLocationGranted: Boolean,
) {
    fun getNextPermissionRequest(): PermissionDestination {
        return when {
            !foregroundLocationGranted || !activityTrackingGranted -> PermissionDestination.FOREGROUND_AND_TRACKING
            !backgroundLocationGranted -> PermissionDestination.BACKGROUND
            else -> PermissionDestination.PASS
        }
    }
}

enum class PermissionDestination {
    PASS, FOREGROUND_AND_TRACKING, BACKGROUND
}