package com.hypertrack.android.ui.screens.sign_in.use_case

import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavDirections
import com.hypertrack.android.deeplink.DeeplinkParams
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.postValue
import com.hypertrack.android.ui.screens.sign_in.SignInFragmentDirections
import com.hypertrack.android.use_case.app.LogExceptionToCrashlyticsUseCase
import com.hypertrack.android.utils.AbstractResult
import com.hypertrack.android.utils.LoadingState
import com.hypertrack.android.utils.SimpleResult
import com.hypertrack.android.utils.tryAsSimpleResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class ConfirmationRequiredUseCase(
    private val destination: MutableLiveData<Consumable<NavDirections>>
) {

    fun execute(login: String): Flow<SimpleResult> {
        return {
            tryAsSimpleResult {
                destination.postValue(
                    SignInFragmentDirections.actionSignInFragmentToConfirmFragment(
                        login
                    )
                )
            }
        }.asFlow()
    }

}

