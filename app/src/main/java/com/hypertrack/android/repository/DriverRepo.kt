package com.hypertrack.android.repository

import android.util.Log
import com.hypertrack.android.utils.AccountDataStorage
import com.hypertrack.android.utils.CrashReportsProvider

class DriverRepo(
    private var _driver: Driver,
    private val accountDataStorage: AccountDataStorage,
    private val crashReportsProvider: CrashReportsProvider
) {

    var driverId: String
        get() = _driver.driverId
        set(value) {
            Log.d(TAG, "New driverId value $value")
            _driver = Driver(value)
            accountDataStorage.saveDriver(_driver)
            crashReportsProvider.setUserIdentifier(value)
        }

    val hasDriverId: Boolean
        get() = _driver.driverId.isNotEmpty()

    companion object {
        const val TAG = "DriverRepo"
    }
}

data class Driver(val driverId : String)
