package com.hypertrack.android.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hypertrack.android.api.ApiClient
import com.hypertrack.android.models.Integration
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.common.DataPage
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.ResultError
import com.hypertrack.android.utils.ResultSuccess
import com.hypertrack.android.utils.ResultValue
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.asFailure
import com.hypertrack.android.utils.asSuccess
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.lang.RuntimeException
import kotlin.coroutines.coroutineContext

interface IntegrationsRepository {
    suspend fun hasIntegrations(): Result<Boolean>
    suspend fun getIntegrations(query: String): Result<DataPage<Integration>>
    fun invalidateCache()
}

class IntegrationsRepositoryImpl(
    private val apiClient: ApiClient
) : IntegrationsRepository {

    private var firstPage: List<Integration>? = null

    override suspend fun hasIntegrations(): Result<Boolean> {
        if (firstPage == null) {
            try {
                firstPage = apiClient.getIntegrations(limit = 100)
                return Success(firstPage!!.isNotEmpty())
            } catch (e: Exception) {
                return Failure(e)
            }
        } else {
            return Success(firstPage!!.isNotEmpty())
        }
    }

    override suspend fun getIntegrations(query: String): Result<DataPage<Integration>> {
        //todo pagination
        if (query.isBlank() && firstPage != null) {
            return DataPage(firstPage!!, null).asSuccess()
        }

        return try {
            apiClient.getIntegrations(query, limit = 100).filter {
                it.name != null
            }.let {
                DataPage(it, null).asSuccess()
            }
        } catch (e: Exception) {
            e.asFailure()
        }
    }

    override fun invalidateCache() {
        firstPage = null
    }

    fun logState(): Map<String, Any> {
        return mapOf(
            "has" to firstPage?.isNotEmpty().toString(),
            "firstPageSize" to (firstPage ?: listOf()).size
        )
    }

}
