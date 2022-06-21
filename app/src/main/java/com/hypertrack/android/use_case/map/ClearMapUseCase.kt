package com.hypertrack.android.use_case.map

import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.toFlow
import com.hypertrack.android.utils.tryAsResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

class ClearMapUseCase {

    // have to be executed on the main thread
    fun execute(map: HypertrackMapWrapper): Flow<Result<Unit>> {
        return tryAsResult { map.clear() }.toFlow()
    }

}
