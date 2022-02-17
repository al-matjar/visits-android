package com.hypertrack.android.use_case.permissions

sealed class PermissionsResult
object AllGranted : PermissionsResult()
object LocationOrActivityNotGranted : PermissionsResult()
object BackgroundLocationNotGranted : PermissionsResult()
