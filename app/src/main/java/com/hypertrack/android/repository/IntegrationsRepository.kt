package com.hypertrack.android.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hypertrack.android.api.ApiClient
import com.hypertrack.android.models.Integration
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.ResultEmptySuccess
import com.hypertrack.android.utils.ResultError
import com.hypertrack.android.utils.ResultSuccess
import com.hypertrack.android.utils.ResultValue
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.lang.RuntimeException
import kotlin.coroutines.coroutineContext

interface IntegrationsRepository {
    val errorFlow: Flow<Consumable<Exception>>
    suspend fun hasIntegrations(): ResultValue<Boolean>
    suspend fun getIntegrations(query: String): List<Integration>
    fun invalidateCache()
}

class IntegrationsRepositoryImpl(
    private val apiClient: ApiClient
) : IntegrationsRepository {

    private var firstPage: List<Integration>? = null

    override val errorFlow = MutableSharedFlow<Consumable<Exception>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override suspend fun hasIntegrations(): ResultValue<Boolean> {
        if (firstPage == null) {
            try {
                firstPage = apiClient.getIntegrations(limit = 100)
                return ResultSuccess(firstPage!!.isNotEmpty())
            } catch (e: Exception) {
                errorFlow.emit(Consumable(e))
                return ResultError(e)
            }
        } else {
            return ResultSuccess(firstPage!!.isNotEmpty())
        }
    }

    override suspend fun getIntegrations(query: String): List<Integration> {
        //todo pagination
        if (query.isBlank() && firstPage != null) {
            return firstPage!!
        }

        try {
            return apiClient.getIntegrations(query, limit = 100).filter {
                it.name != null
            }
        } catch (e: Exception) {
            errorFlow.emit(Consumable(e))
            return listOf()
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
