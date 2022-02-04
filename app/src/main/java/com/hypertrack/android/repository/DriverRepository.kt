package com.hypertrack.android.repository

import com.hypertrack.android.data.AccountDataStorage
import com.hypertrack.android.ui.common.util.isEmail
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.ServiceLocator
import com.squareup.moshi.JsonClass
import java.util.*
import javax.inject.Provider

class DriverRepository(
    private val accountRepositoryProvider: Provider<AccountRepository>,
    private val serviceLocator: ServiceLocator,
    private val accountDataStorage: AccountDataStorage,
    private val osUtilsProvider: OsUtilsProvider,
    private val crashReportsProvider: CrashReportsProvider,
) {
    private var _user: User? = null
    val user: User?
        get() = _user

    init {
        _user = accountDataStorage.loadUser() ?: accountDataStorage.getDriverValue().let {
            if (it.driverId.isBlank()) {
                null
            } else {
                it
            }
        }?.toUser()
    }

    fun setUserData(
        metadata: Map<String, Any>? = null,
        email: String? = null,
        phoneNumber: String? = null,
        deeplinkWithoutGetParams: String? = null,
        driverId: String? = null
    ) {
        check(email != null || phoneNumber != null || driverId != null) {
            "User data must have email or phone number or driver id"
        }
        _user = User(
            email = email,
            phoneNumber = phoneNumber,
            metadata = metadata,
            driverId = driverId
        ).also {
            accountDataStorage.saveUser(it)
        }
        serviceLocator.getHyperTrackService(accountRepositoryProvider.get().publishableKey).apply {
            val name = metadata?.get("name").stringOrNull()
                ?: email?.split("@")?.first()?.capitalize(Locale.ROOT)
                ?: phoneNumber
                ?: if (osUtilsProvider.isEmail(driverId)) {
                    driverId!!.split("@").first().capitalize(Locale.ROOT)
                } else driverId

            setDeviceInfo(
                name = name,
                email = email,
                phoneNumber = phoneNumber,
                driverId = driverId,
                metadata = metadata,
                deeplinkWithoutGetParams = deeplinkWithoutGetParams
            )
        }
    }

    val hasUserData: Boolean
        get() = _user != null

    val username: String
        get() = _user!!.let {
            it.email ?: it.phoneNumber ?: it.driverId ?: ""
        }

    companion object {
        const val TAG = "DriverRepo"
    }
}

@JsonClass(generateAdapter = true)
data class User(
    val email: String? = null,
    val phoneNumber: String? = null,
    val metadata: Map<String, Any>? = null,
    //legacy
    val driverId: String? = null,
)

//todo remove (v0.9.20)
@JsonClass(generateAdapter = true)
data class Driver(
    val driverId: String
) {
    fun toUser(): User {
        return User(
            driverId = driverId,
            email = if (driverId.isEmail()) driverId else null
        )
    }
}

fun Any?.stringOrNull(): String? {
    if (this is String) {
        return this
    } else {
        return null
    }
}
