package com.hypertrack.android.di

import android.app.Application
import com.hypertrack.android.interactors.app.InitAppAction
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.interactors.app.state.AppNotInitialized
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.TrackingStateChangedAction
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.interactors.app.state.UserNotLoggedIn
import com.hypertrack.android.ui.activity.ActivityViewModelFactory
import com.hypertrack.android.ui.common.ParamViewModelFactory
import com.hypertrack.android.ui.common.Tab
import com.hypertrack.android.ui.common.UserScopeViewModelFactory
import com.hypertrack.android.ui.common.ViewModelFactory
import com.hypertrack.android.ui.common.util.requireValue
import com.hypertrack.android.use_case.app.AppCreationUseCase
import com.hypertrack.android.use_case.sdk.TrackingState
import com.hypertrack.android.utils.crashlytics.BaseCrashReportsProvider
import com.hypertrack.android.utils.crashlytics.FirebaseCrashReportsProvider
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.ResourceProvider
import com.hypertrack.android.utils.crashlytics.DoubleCrashReportsProvider
import com.hypertrack.android.utils.crashlytics.SentryCrashReportsProvider

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
object Injector {

    //should be static to enable reliable exception reporting in all places in the code
    lateinit var crashReportsProvider: BaseCrashReportsProvider
    private lateinit var resourceProvider: ResourceProvider

    private val trackingStateListener = { trackingState: TrackingState ->
        crashReportsProvider.log("trackingStateListener $trackingState")
        appInteractor.handleAction(TrackingStateChangedAction(trackingState))
    }

    private lateinit var appInteractor: AppInteractor

    fun appOnCreate(application: Application) {
        crashReportsProvider = BaseCrashReportsProvider(
            DoubleCrashReportsProvider(
                SentryCrashReportsProvider(application),
                FirebaseCrashReportsProvider(application)
            )
        )
        resourceProvider = OsUtilsProvider(application, crashReportsProvider)
        val appScope = AppCreationUseCase().execute(
            application,
            crashReportsProvider,
            trackingStateListener
        )
        appInteractor = AppInteractor(appScope)
        appInteractor.handleAction(InitAppAction(application))
    }

    fun provideActivityViewModelFactory(): ActivityViewModelFactory {
        return when (val state = appInteractor.appState.requireValue()) {
            is AppNotInitialized -> {
                ActivityViewModelFactory(
                    appInteractor,
                    state.appScope,
                    state.useCases
                )
            }
            is AppInitialized -> {
                ActivityViewModelFactory(
                    appInteractor,
                    state.appScope,
                    state.useCases,
                )
            }
        }
    }

    fun provideViewModelFactory(): ViewModelFactory {
        return when (val state = appInteractor.appState.requireValue()) {
            is AppNotInitialized -> {
                ViewModelFactory(appInteractor, state.appScope, state.useCases)
            }
            is AppInitialized -> {
                ViewModelFactory(appInteractor, state.appScope, state.useCases)
            }
        }
    }

    // todo remove (separate factory for each vm)
    fun <T> provideUserScopeParamViewModelFactory(param: T): ParamViewModelFactory<T> {
        return when (val state = appInteractor.appState.requireValue()) {
            is AppNotInitialized -> {
                throw IllegalStateException("App is not initialized")
            }
            is AppInitialized -> {
                when (state.userState) {
                    is UserLoggedIn -> {
                        ParamViewModelFactory(
                            param,
                            appInteractor,
                            state.appScope,
                            state.userState.userScope,
                            state.useCases
                        )
                    }
                    UserNotLoggedIn -> {
                        throw IllegalStateException("User is not logged in")
                    }
                }
            }
        }
    }

    fun provideUserScopeViewModelFactory(): UserScopeViewModelFactory {
        return when (val state = appInteractor.appState.requireValue()) {
            is AppNotInitialized -> {
                throw IllegalStateException("App is not initialized")
            }
            is AppInitialized -> {
                when (state.userState) {
                    is UserLoggedIn -> {
                        state.userState.userScope.userScopeViewModelFactory
                    }
                    UserNotLoggedIn -> {
                        throw IllegalStateException("User is not logged in")
                    }
                }
            }
        }
    }

    fun provideTabs(): List<Tab> = mutableListOf<Tab>().apply {
        addAll(
            listOf(
                Tab.CURRENT_TRIP,
                Tab.HISTORY,
            )
        )
        add(Tab.ORDERS)
        addAll(
            listOf(
                Tab.PLACES,
                Tab.SUMMARY,
                Tab.PROFILE,
            )
        )
    }

    fun provideAppInteractor(): AppInteractor {
        return appInteractor
    }

}

