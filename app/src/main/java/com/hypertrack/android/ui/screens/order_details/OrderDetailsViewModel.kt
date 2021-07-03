package com.hypertrack.android.ui.screens.order_details

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MarkerOptions
import com.hypertrack.android.api.*
import com.hypertrack.android.interactors.*
import com.hypertrack.android.models.local.LocalOrder
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.repository.AccountRepository
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.ErrorHandler
import com.hypertrack.android.ui.base.ZipLiveData
import com.hypertrack.android.ui.common.KeyValueItem
import com.hypertrack.android.ui.common.delegates.OrderAddressDelegate
import com.hypertrack.android.ui.common.format
import com.hypertrack.android.ui.common.requireValue
import com.hypertrack.android.ui.screens.visit_details.VisitDetailsFragment
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.Exception

class OrderDetailsViewModel(
    private val orderId: String,
    private val tripsInteractor: TripsInteractor,
    private val photoUploadInteractor: PhotoUploadQueueInteractor,
    private val osUtilsProvider: OsUtilsProvider,
    private val accountRepository: AccountRepository,
    private val apiClient: ApiClient,
    private val globalScope: CoroutineScope
) : BaseViewModel(osUtilsProvider) {

    private val addressDelegate = OrderAddressDelegate(osUtilsProvider)

    override val errorHandler =
        ErrorHandler(osUtilsProvider, tripsInteractor.errorFlow.asLiveData())
    private val map = MutableLiveData<GoogleMap>()

    private val order = tripsInteractor.getOrderLiveData(orderId)

    val address = Transformations.map(order) { addressDelegate.fullAddress(it) }
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
        //todo check leaks
        Transformations.map(order) { it.note ?: it.metadataNote }.observeForever {
            note.postValue(it)
        }
    }

    val metadata = Transformations.map(order) { order ->
        order.metadata
            .toMutableMap().apply {
                put(osUtilsProvider.stringFromResource(R.string.order_id), orderId)
                put(osUtilsProvider.stringFromResource(R.string.order_status), order.status.value)
                put(
                    osUtilsProvider.stringFromResource(R.string.coordinates),
                    order.destinationLatLng.format()
                )
                if (accountRepository.isPickUpAllowed && order.status == OrderStatus.ONGOING) {
                    put(
                        osUtilsProvider.stringFromResource(R.string.order_picked_up),
                        order.isPickedUp.toString()
                    )
                }
            }.map {
                KeyValueItem(it.key, it.value)
            }
    }
    val showPhotosGroup = Transformations.map(order) {
        it.legacy
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
    val showPickUpButton = Transformations.map(order) {
        it.legacy && !it.isPickedUp && it.status == OrderStatus.ONGOING && accountRepository.isPickUpAllowed
    }
    val externalMapsIntent = MutableLiveData<Consumable<Intent>>()

    init {
        ZipLiveData(order, map).apply {
            //todo check leaks
            observeForever {
                displayOrderLocation(it.first, it.second)
            }
        }
    }

    private var currentPhotoPath: String? = null

    @SuppressLint("MissingPermission")
    fun onMapReady(googleMap: GoogleMap) {
        googleMap.uiSettings.apply {
            isScrollGesturesEnabled = false
            isMyLocationButtonEnabled = true
            isZoomControlsEnabled = true
        }
        try {
            googleMap.isMyLocationEnabled = true
        } catch (_: Exception) {
        }
        map.postValue(googleMap)

    }

    private fun displayOrderLocation(order: LocalOrder, googleMap: GoogleMap) {
        googleMap.addMarker(
            MarkerOptions().position(order.destinationLatLng)
                .title(addressDelegate.shortAddress(order))
        )
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(order.destinationLatLng, 13.0f))
    }

    fun onCancelClicked(note: String? = null) {
        onOrderCompleteAction(true, note)
    }

    fun onCompleteClicked(note: String? = null) {
        onOrderCompleteAction(false, note)
    }

    fun onExit(orderNote: String) {
        globalScope.launch {
            tripsInteractor.updateOrderNote(orderId, orderNote)
        }
    }

    fun onCopyClick(it: String) {
        osUtilsProvider.copyToClipboard(it)
    }

    fun onPickUpClicked() {
        viewModelScope.launch {
            tripsInteractor.setOrderPickedUp(orderId)
        }
    }

    fun onAddPhotoClicked(activity: Activity, note: String) {
        globalScope.launch {
            tripsInteractor.updateOrderNote(orderId, note)
        }
        try {
            val file = osUtilsProvider.createImageFile()
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = file.absolutePath

            activity.startActivityForResult(
                osUtilsProvider.createTakePictureIntent(activity, file),
                VisitDetailsFragment.REQUEST_IMAGE_CAPTURE
            )
        } catch (e: Exception) {
            errorHandler.postText(R.string.cannot_create_file_msg)
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == VisitDetailsFragment.REQUEST_IMAGE_CAPTURE && resultCode == AppCompatActivity.RESULT_OK) {
            viewModelScope.launch {
                currentPhotoPath?.let {
                    loadingStateBase.postValue(true)
                    tripsInteractor.addPhotoToOrder(
                        orderId,
                        currentPhotoPath!!
                    )
                    loadingStateBase.postValue(false)
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
            loadingStateBase.postValue(true)
            note?.let {
                tripsInteractor.updateOrderNote(orderId, it)
            }
            val res = if (!cancel) {
                tripsInteractor.completeOrder(order.value!!.id)
            } else {
                tripsInteractor.cancelOrder(order.value!!.id)
            }
            handleOrderCompletionResult(res)
            loadingStateBase.postValue(false)
        }
    }

    private fun handleOrderCompletionResult(res: OrderCompletionResponse) {
        when (res) {
            OrderCompletionCompleted -> {
                errorHandler.postText(R.string.order_already_completed)
            }
            OrderCompletionCanceled -> {
                errorHandler.postText(R.string.order_already_canceled)
            }
            is OrderCompletionFailure -> {
                errorHandler.postException(res.exception)
            }
            else -> {
            }
        }
    }

    private fun updatePhotos(order: LocalOrder, uploadQueue: Map<String, PhotoForUpload>) {
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

}