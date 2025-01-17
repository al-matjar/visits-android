package com.hypertrack.android.ui.screens.order_details

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import com.google.android.gms.maps.GoogleMap
import com.hypertrack.android.api.*
import com.hypertrack.android.interactors.*
import com.hypertrack.android.interactors.trip.NotClockedInException
import com.hypertrack.android.interactors.trip.TripsInteractor
import com.hypertrack.android.models.local.Order
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.ui.MainActivity.Companion.REQUEST_CODE_IMAGE_CAPTURE
import com.hypertrack.android.ui.base.*
import com.hypertrack.android.ui.common.adapters.KeyValueItem
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map.MapParams
import com.hypertrack.android.ui.common.util.format
import com.hypertrack.android.ui.common.util.nullIfBlank
import com.hypertrack.android.ui.common.util.requireValue
import com.hypertrack.android.utils.JustFailure
import com.hypertrack.android.utils.JustSuccess
import com.hypertrack.android.utils.formatters.DateTimeFormatter
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.launch
import kotlin.Exception

class OrderDetailsViewModel(
    private val orderId: String,
    baseDependencies: BaseViewModelDependencies,
    private val tripsInteractor: TripsInteractor,
    private val photoUploadInteractor: PhotoUploadQueueInteractor,
    private val dateTimeFormatter: DateTimeFormatter,
) : BaseViewModel(baseDependencies) {

    private val map = MutableLiveData<HypertrackMapWrapper>()

    private val order = tripsInteractor.getOrderLiveData(orderId)

    val address = Transformations.map(order) {
        it.fullAddress ?: resourceProvider.stringFromResource(R.string.address_not_available)
    }
    val photos = MediatorLiveData<List<PhotoItem>>().apply {
        addSource(order) {
            updatePhotos(it, photoUploadInteractor.queue.value!!)
        }
        addSource(photoUploadInteractor.queue) { queue ->
            order.value?.let { updatePhotos(it, queue) }
        }
    }

    val note = MutableLiveData<String?>()

    init {
        Transformations.map(order) { it.note ?: it.metadataNote }.observeManaged {
            note.postValue(it)
        }

        tripsInteractor.errorFlow.asLiveData().observeManaged { consumable ->
            consumable.consume { onError(it) }
        }
    }

    val metadata = Transformations.map(order) { order ->
        order.metadata
            .toMutableMap().apply {
                put(osUtilsProvider.stringFromResource(R.string.order_id), orderId)
                put(osUtilsProvider.stringFromResource(R.string.order_status), order.status.value)
                order.completedAt?.let {
                    put(
                        osUtilsProvider.stringFromResource(R.string.order_completed_at),
                        dateTimeFormatter.formatDateTime(it)
                    )
                }
                order.scheduledAt?.let {
                    put(
                        osUtilsProvider.stringFromResource(R.string.order_scheduled_at),
                        dateTimeFormatter.formatDateTime(it)
                    )
                }
                put(
                    osUtilsProvider.stringFromResource(R.string.coordinates),
                    order.destinationLatLng.format()
                )
            }.map {
                KeyValueItem(it.key, it.value)
            }
    }
    val showAddPhoto = Transformations.map(order) {
        it.status == OrderStatus.ONGOING
    }
    val isNoteEditable = Transformations.map(order) {
        it.status == OrderStatus.ONGOING
    }
    val showCompleteButtons = Transformations.map(order) {
        it.status == OrderStatus.ONGOING
    }
    val showSnoozeButton = Transformations.map(order) {
        it.status == OrderStatus.ONGOING
    }
    val showUnSnoozeButton = Transformations.map(order) {
        it.status == OrderStatus.SNOOZED
    }
    val externalMapsIntent = MutableLiveData<Consumable<Intent>>()

    //todo test
    val showNote = MediatorLiveData<Boolean>().apply {
        addSource(isNoteEditable) {
            postValue(shouldShowNote(it, note.value))
        }
        addSource(note) {
            postValue(shouldShowNote(isNoteEditable.value, it))
        }
    }

    init {
        ZipNotNullableLiveData(order, map).apply {
            //todo check leaks
            observeForever {
                displayOrderLocation(it.first, it.second)
            }
        }
    }

    private var currentPhotoPath: String? = null

    @SuppressLint("MissingPermission")
    fun onMapReady(googleMap: GoogleMap) {
        map.postValue(
            HypertrackMapWrapper(
                googleMap, osUtilsProvider, crashReportsProvider, MapParams(
                    enableScroll = false,
                    enableZoomKeys = true,
                    enableMyLocationButton = false,
                    enableMyLocationIndicator = true
                )
            )
        )
    }

    private fun displayOrderLocation(order: Order, mapWrapper: HypertrackMapWrapper) {
        mapWrapper.addOrder(order)
        mapWrapper.moveCamera(order.destinationLatLng, 13.0f)
    }

    fun onCancelClicked(note: String? = null) {
        onOrderCompleteAction(true, note)
    }

    fun onCompleteClicked(note: String? = null) {
        onOrderCompleteAction(false, note)
    }

    fun onExit(orderNote: String) {
        tripsInteractor.updateOrderNoteAsync(orderId, orderNote)
    }

    fun onCopyClick(it: String) {
        osUtilsProvider.copyToClipboard(it)
    }

    fun onSnoozeClicked() {
        withLoadingStateAndErrorHandler {
            tripsInteractor.snoozeOrder(orderId).let {
                when (it) {
                    JustSuccess -> {
                    }
                    is JustFailure -> {
                        showExceptionMessageAndReport(it.exception)
                    }
                }
            }
        }
    }

    fun onUnSnoozeClicked() {
        withLoadingStateAndErrorHandler {
            tripsInteractor.unSnoozeOrder(orderId).let {
                when (it) {
                    JustSuccess -> {
                    }
                    is JustFailure -> showExceptionMessageAndReport(it.exception)
                }
            }
        }
    }

    fun onAddPhotoClicked(activity: Activity, note: String) {
        tripsInteractor.updateOrderNoteAsync(orderId, note)
        try {
            val file = osUtilsProvider.createImageFile()
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = file.absolutePath

            activity.startActivityForResult(
                osUtilsProvider.createTakePictureIntent(activity, file),
                REQUEST_CODE_IMAGE_CAPTURE
            )
        } catch (e: Exception) {
            showError(R.string.cannot_create_file_msg)
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_IMAGE_CAPTURE && resultCode == AppCompatActivity.RESULT_OK) {
            viewModelScope.launch {
                currentPhotoPath?.let {
                    loadingState.postValue(true)
                    tripsInteractor.addPhotoToOrder(
                        orderId,
                        currentPhotoPath!!
                    )
                    loadingState.postValue(false)
                }
            }
        }
    }

    fun onPhotoClicked(photoId: String) {
        if (photos.value!!.firstOrNull { it.photoId == photoId }?.state == PhotoUploadingState.ERROR) {
            photoUploadInteractor.retry(photoId)
        }
    }

    fun onDirectionsClick() {
        val intent = osUtilsProvider.getMapsIntent(order.value!!.destinationLatLng)
        intent?.let {
            externalMapsIntent.postValue(Consumable(it))
        }
    }

    fun onCopyAddressClick() {
        osUtilsProvider.copyToClipboard(address.requireValue())
    }

    private fun onOrderCompleteAction(cancel: Boolean, note: String?) {
        viewModelScope.launch {
            loadingState.postValue(true)
            note?.let {
                tripsInteractor.updateOrderNote(orderId, it)
            }
            val res = if (!cancel) {
                tripsInteractor.completeOrder(order.value!!.id)
            } else {
                tripsInteractor.cancelOrder(order.value!!.id)
            }
            handleOrderCompletionResult(res)
            loadingState.postValue(false)
        }
    }

    private fun handleOrderCompletionResult(res: OrderCompletionResponse) {
        when (res) {
            OrderCompletionCompleted -> {
                showError(R.string.order_already_completed)
            }
            OrderCompletionCanceled -> {
                showError(R.string.order_already_canceled)
            }
            is OrderCompletionFailure -> {
                if (res.exception is NotClockedInException) {
                    showError(R.string.order_not_clocked_in)
                } else {
                    showExceptionMessageAndReport(res.exception)
                }
            }
            else -> {
            }
        }
    }

    private fun updatePhotos(order: Order, uploadQueue: Map<String, PhotoForUpload>) {
        photos.postValue(
            order.photos
                .map { photo ->
                    val photoId = photo.photoId
                    return@map uploadQueue.get(photoId).let { photoFromQueue ->
                        if (photoFromQueue != null) {
                            PhotoItem(
                                photoId = photoId,
                                photoFromQueue.base64thumbnail?.let {
                                    osUtilsProvider.decodeBase64Bitmap(
                                        it
                                    )
                                },
                                photoFromQueue.state
                            )
                        } else {
                            PhotoItem(
                                photoId = photoId,
                                photo.base64thumbnail?.let {
                                    osUtilsProvider.decodeBase64Bitmap(
                                        it
                                    )
                                },
                                PhotoUploadingState.UPLOADED
                            )
                        }
                    }
                }
        )
    }

    private fun shouldShowNote(isEditable: Boolean?, note: String?): Boolean {
        return isEditable == true || note.nullIfBlank() != null
    }

}

