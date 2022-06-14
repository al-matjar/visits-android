package com.hypertrack.android.ui.screens.order_details

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.hypertrack.android.di.Injector
import com.hypertrack.android.ui.base.ProgressDialogFragment
import com.hypertrack.android.ui.common.adapters.KeyValueAdapter
import com.hypertrack.android.ui.common.util.SnackBarUtil
import com.hypertrack.android.ui.common.util.observeWithErrorHandling
import com.hypertrack.android.ui.common.util.setGoneState
import com.hypertrack.android.ui.common.util.setLinearLayoutManager
import com.hypertrack.android.ui.common.util.textString
import com.hypertrack.android.utils.stringFromResource
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_order_detail.*
import kotlinx.android.synthetic.main.fragment_order_detail.bDirections
import kotlinx.android.synthetic.main.fragment_order_detail.divider
import kotlinx.android.synthetic.main.fragment_order_detail.etVisitNote
import kotlinx.android.synthetic.main.fragment_order_detail.ivBack
import kotlinx.android.synthetic.main.fragment_order_detail.rvMetadata
import kotlinx.android.synthetic.main.fragment_order_detail.rvPhotos
import kotlinx.android.synthetic.main.fragment_order_detail.tvAddress
import kotlinx.android.synthetic.main.fragment_order_detail.tvCancel
import kotlinx.android.synthetic.main.fragment_order_detail.tvTakePicture


class OrderDetailsFragment : ProgressDialogFragment(R.layout.fragment_order_detail) {

    private val args: OrderDetailsFragmentArgs by navArgs()
    private val vm: OrderDetailsViewModel by viewModels {
        Injector.provideUserScopeParamViewModelFactory(
            args.orderId
        )
    }
    private lateinit var map: GoogleMap

    private val metadataAdapter = KeyValueAdapter(showCopyButton = true)
    private val photosAdapter = PhotosAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync {
            vm.onMapReady(it)
        }

        rvMetadata.setLinearLayoutManager(requireContext())
        rvMetadata.adapter = metadataAdapter
        metadataAdapter.onCopyClickListener = {
            vm.onCopyClick(it)
        }

        rvPhotos.layoutManager = LinearLayoutManager(requireContext(), HORIZONTAL, false)
        rvPhotos.adapter = photosAdapter
        photosAdapter.onItemClickListener = {
            vm.onPhotoClicked(it.photoId)
        }

        vm.address.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            tvAddress.text = it
        }

        vm.metadata.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            metadataAdapter.updateItems(it)
        }

        vm.note.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            etVisitNote.setText(it)
        }

        vm.showCompleteButtons.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            orderCompletionGroup.setGoneState(!it)
        }

        vm.showSnoozeButton.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            tvSnooze.setGoneState(!it)
            divider.setGoneState(!it)
        }

        vm.showUnSnoozeButton.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            tvUnsnooze.setGoneState(!it)
            divider.setGoneState(!it)
        }

        vm.showAddPhoto.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            photosGroup.setGoneState(!it)
        }

        vm.isNoteEditable.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            etVisitNote.isEnabled = it
        }

        vm.showNote.observeWithErrorHandling(viewLifecycleOwner, vm::onError) { show ->
            listOf(etVisitNote, ivVisitNote).forEach { it.setGoneState(!show) }
        }

        vm.loadingState.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            if (it) showProgress() else dismissProgress()
        }

        vm.showErrorMessageEvent.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            SnackBarUtil.showErrorSnackBar(view, it)
        }

        vm.photos.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            displayPhotos(it)
        }

        vm.externalMapsIntent.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            it.consume {
                mainActivity().startActivity(it)
            }
        }

        tvTakePicture.setOnClickListener {
            vm.onAddPhotoClicked(mainActivity(), etVisitNote.textString())
        }

        ivBack.setOnClickListener {
            mainActivity().onBackPressed()
        }

        tvComplete.setOnClickListener {
            createCompleteDialog().show()
        }

        tvCancel.setOnClickListener {
            createCancelDialog().show()
        }

        tvSnooze.setOnClickListener {
            createSnoozeDialog().show()
        }

        tvUnsnooze.setOnClickListener {
            vm.onUnSnoozeClicked()
        }

        bDirections.setOnClickListener {
            vm.onDirectionsClick()
        }

        bCopyAddress.setOnClickListener {
            vm.onCopyAddressClick()
        }
    }

    private fun createCancelDialog(): AlertDialog {
        return AlertDialog.Builder(requireContext())
            .setMessage(
                R.string.order_cancel_confirmation.stringFromResource()
                    .let { createBoldSpannable(it, it.indexOf("CANCEL"), "CANCEL".length) })
            .setPositiveButton(R.string.yes, DialogInterface.OnClickListener { dialog, which ->
                vm.onCancelClicked(etVisitNote.textString())
            })
            .setNegativeButton(R.string.no, null)
            .create()
    }

    private fun createCompleteDialog(): AlertDialog {
        return AlertDialog.Builder(requireContext())
            .setMessage(
                R.string.order_complete_confirmation.stringFromResource()
                    .let { createBoldSpannable(it, it.indexOf("COMPLETE"), "COMPLETE".length) })
            .setPositiveButton(R.string.yes, DialogInterface.OnClickListener { dialog, which ->
                vm.onCompleteClicked(etVisitNote.textString())
            })
            .setNegativeButton(R.string.no, null)
            .create()
    }

    private fun createSnoozeDialog(): AlertDialog {
        return AlertDialog.Builder(requireContext())
            .setMessage(
                R.string.order_snooze_confirmation.stringFromResource()
                    .let { createBoldSpannable(it, it.indexOf("SNOOZE"), "SNOOZE".length) })
            .setPositiveButton(R.string.yes, DialogInterface.OnClickListener { dialog, which ->
                vm.onSnoozeClicked()
            })
            .setNegativeButton(R.string.no, null)
            .create()
    }

    private fun createBoldSpannable(str: String, start: Int, length: Int): SpannableStringBuilder {
        val sb = SpannableStringBuilder(str)

        sb.setSpan(
            StyleSpan(Typeface.BOLD),
            start,
            start + length,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        );

        return sb
    }

    private fun displayPhotos(photos: List<PhotoItem>) {
        rvPhotos.setGoneState(photos.isEmpty())
        photosAdapter.updateItems(photos)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        vm.onActivityResult(requestCode, resultCode, data)
    }

    override fun onPause() {
        vm.onExit(etVisitNote.textString())
        super.onPause()
    }

    override fun onBackPressed(): Boolean {
        vm.onExit(etVisitNote.textString())
        return super.onBackPressed()
    }
}
