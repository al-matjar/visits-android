package com.hypertrack.android.interactors.app.effect.navigation

import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavDirections
import com.hypertrack.android.interactors.app.AppAction
import com.hypertrack.android.interactors.app.AppErrorAction
import com.hypertrack.android.interactors.app.noAction
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.toConsumable
import com.hypertrack.android.ui.common.util.updateConsumableAsFlow
import com.hypertrack.android.use_case.app.UseCases
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.toFlow
import com.hypertrack.logistics.android.github.NavGraphDirections
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

@Suppress("OPT_IN_USAGE")
class NavigationEffectHandler(
    private val useCases: UseCases,
    private val navigationEventLiveData: MutableLiveData<Consumable<NavDirections>>
) {

    fun applyEffect(effect: NavigationEffect): Flow<AppAction?> {
        return when (effect) {
            is NavigateInGraphEffect -> {
                navigationEventLiveData.updateConsumableAsFlow(effect.destination).noAction()
            }
            is NavigateToUserScopeScreensEffect -> {
                useCases.navigateToUserScopeScreensUseCase.execute(
                    effect.userState.userScope.permissionsInteractor
                ).flatMapConcat {
                    when (it) {
                        is Success -> {
                            applyEffect(NavigateInGraphEffect(it.data))
                        }
                        is Failure -> {
                            AppErrorAction(it.exception).toFlow()
                        }
                    }
                }
            }
            is NavigateToSignInEffect -> {
                applyEffect(NavigateInGraphEffect(NavGraphDirections.actionGlobalSignInFragment()))
            }
        }
    }

}
