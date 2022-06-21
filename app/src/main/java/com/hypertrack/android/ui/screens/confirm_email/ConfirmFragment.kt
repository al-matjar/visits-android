package com.hypertrack.android.ui.screens.confirm_email

import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.core.view.doOnLayout
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.hypertrack.android.di.Injector
import com.hypertrack.android.interactors.app.RegisterScreenAction
import com.hypertrack.android.interactors.app.state.ConfirmEmailScreen
import com.hypertrack.android.ui.base.ProgressDialogFragment
import com.hypertrack.android.ui.base.navigate
import com.hypertrack.android.ui.common.util.SnackBarUtil
import com.hypertrack.android.ui.common.util.Utils
import com.hypertrack.android.ui.common.util.observeWithErrorHandling
import com.hypertrack.android.ui.views.VerificationCodeView
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_confirm.*


class ConfirmFragment : ProgressDialogFragment(R.layout.fragment_confirm) {

    private val args: ConfirmFragmentArgs by navArgs()

    private val vm: ConfirmEmailViewModel by viewModels {
        Injector.provideViewModelFactory()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Injector.provideAppInteractor().handleAction(RegisterScreenAction(ConfirmEmailScreen))

        vm.init(args.email)

        vm.loadingState.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            if (it) showProgress() else dismissProgress()
        }

        vm.proceedButtonEnabled.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            verified.isSelected = it
            verified.isEnabled = it
        }

        vm.showErrorMessageEvent.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            SnackBarUtil.showErrorSnackBar(view, it)
        }

        vm.destination.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            findNavController().navigate(it)
        }

        vm.clipboardCode.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            verificationCode.code = it
            Toast.makeText(requireContext(), R.string.code_from_clipboard, LENGTH_SHORT).show()
            Utils.hideKeyboard(mainActivity())
        }

        view.doOnLayout {
            vm.onClipboardReady()
        }

        tvEmail.text = args.email

        verificationCode.listener = object : VerificationCodeView.VerificationCodeListener {
            override fun onCodeChanged(code: String, complete: Boolean) {
                vm.onCodeChanged(code, complete, mainActivity())
            }

            override fun onEnterPressed(complete: Boolean) {
                vm.onVerifiedClick(verificationCode.code, complete, mainActivity())
            }
        }
        Utils.showKeyboard(mainActivity(), verificationCode.etCode)

        verified.setOnClickListener {
            vm.onVerifiedClick(
                verificationCode.code,
                verificationCode.isCodeComplete,
                mainActivity()
            )
        }

        resend.setOnClickListener {
            vm.onResendClick()
        }

    }

}
