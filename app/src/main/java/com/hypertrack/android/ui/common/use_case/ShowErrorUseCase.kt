package com.hypertrack.android.ui.common.use_case

import androidx.lifecycle.MutableLiveData
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.common.use_case.get_error_message.DisplayableError
import com.hypertrack.android.ui.common.use_case.get_error_message.GetErrorMessageUseCase
import com.hypertrack.android.ui.common.util.postValue
import com.hypertrack.android.utils.ErrorMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

@Suppress("OPT_IN_USAGE")
class ShowErrorUseCase(
    private val showErrorMessageEvent: MutableLiveData<Consumable<ErrorMessage>>,
    private val getErrorMessageUseCase: GetErrorMessageUseCase
) {

    fun execute(error: DisplayableError): Flow<Unit> {
        return getErrorMessageUseCase.execute(error)
            .map {
                showErrorMessageEvent.postValue(it)
            }
    }

}
