package com.hypertrack.android.ui.activity.use_case

import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import com.google.android.datatransport.runtime.Destination
import com.hypertrack.android.interactors.app.AppErrorAction
import com.hypertrack.android.ui.MainActivity
import com.hypertrack.android.ui.base.BaseFragment
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.toConsumable
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragment
import com.hypertrack.android.use_case.handle_push.GeofenceVisitNotification
import com.hypertrack.android.use_case.handle_push.OutageNotification
import com.hypertrack.android.use_case.handle_push.TripUpdateNotification
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.JustFailure
import com.hypertrack.android.utils.JustSuccess
import com.hypertrack.android.utils.NotificationUtil
import com.hypertrack.android.utils.NotificationUtil.KEY_NOTIFICATION_DATA
import com.hypertrack.android.utils.NotificationUtil.KEY_NOTIFICATION_TYPE
import com.hypertrack.android.utils.SimpleResult
import com.hypertrack.android.utils.asSimpleFailure
import com.hypertrack.android.utils.asSimpleSuccess
import com.hypertrack.android.utils.asSuccess
import com.hypertrack.logistics.android.github.NavGraphDirections
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@Suppress("OPT_IN_USAGE")
class HandleNotificationClickUseCase(
    private val destination: MutableLiveData<Consumable<NavDirections>>,
) {

    fun execute(intent: Intent, currentFragment: Fragment): Flow<AppErrorAction?> {
        return {
            intent.getStringExtra(KEY_NOTIFICATION_TYPE)?.let { type ->
                when (type) {
                    TripUpdateNotification::class.java.simpleName -> {
                        if (currentFragment is VisitsManagementFragment) {
                            currentFragment.refreshOrders()
                        } else {
                            destination.postValue(
                                NavGraphDirections.actionGlobalVisitManagementFragment()
                                    .toConsumable()
                            )
                        }.asSimpleSuccess()
                    }
                    OutageNotification::class.java.simpleName -> {
                        intent.getParcelableExtra<OutageNotification>(
                            KEY_NOTIFICATION_DATA
                        )?.let { notificationData ->
                            destination.postValue(
                                NavGraphDirections.actionGlobalOutageFragment(notificationData)
                                    .toConsumable()
                            ).asSimpleSuccess()
                        } ?: JustFailure(NullPointerException())
                    }
                    GeofenceVisitNotification::class.java.simpleName -> {
                        intent.getParcelableExtra<GeofenceVisitNotification>(
                            KEY_NOTIFICATION_DATA
                        )?.let { notificationData ->
                            destination.postValue(
                                NavGraphDirections.actionGlobalPlaceDetailsFragment(
                                    notificationData.geofenceId
                                ).toConsumable()
                            ).asSimpleSuccess()
                        } ?: JustFailure(NullPointerException())
                    }
                    else -> IllegalArgumentException(type).asSimpleFailure()
                }
            } ?: JustSuccess
        }.asFlow().onEach {
            intent.removeExtra(KEY_NOTIFICATION_TYPE)
            intent.removeExtra(KEY_NOTIFICATION_DATA)
        }.map {
            when (it) {
                JustSuccess -> null
                is JustFailure -> AppErrorAction(it.exception)
            }
        }
    }

}
