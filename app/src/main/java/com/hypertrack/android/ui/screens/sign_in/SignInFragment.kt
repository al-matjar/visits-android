package com.hypertrack.android.ui.screens.sign_in

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.hypertrack.android.di.Injector
import com.hypertrack.android.interactors.app.RegisterScreenAction
import com.hypertrack.android.interactors.app.state.SignInScreen
import com.hypertrack.android.ui.base.ProgressDialogFragment
import com.hypertrack.android.ui.base.navigate
import com.hypertrack.android.ui.common.util.*
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_signin.*
import kotlinx.android.synthetic.main.inflate_paste_deeplink.*
import kotlinx.android.synthetic.main.inflate_paste_deeplink.view.*

class SignInFragment : ProgressDialogFragment(R.layout.fragment_signin) {

    private val args: SignInFragmentArgs by navArgs()

    private val vm: SignInViewModel by viewModels {
        Injector.provideViewModelFactory()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Injector.provideAppInteractor().handleAction(RegisterScreenAction(SignInScreen))

        vm.viewState.observeWithErrorHandling(viewLifecycleOwner, this::onError) {
            it.isLoginButtonEnabled.let { isClickable ->
                sign_in.isEnabled = isClickable
                sign_in.isSelected = isClickable
            }
            it.showDeeplinkIssuesDialog.let { show ->
                bDeeplinkIssues.setGoneState(show)
                if (show) {
                    lDeeplinkIssues.show()
                    lDeeplinkIssues.alpha = 0f
                    lDeeplinkIssues.animate().setDuration(100L).alpha(1f)
                } else {
                    lDeeplinkIssues.animate().setDuration(100L).withEndAction {
                        lDeeplinkIssues.hide()
                    }.alpha(0f)
                }
            }
            it.hardwareId?.value?.toView(tvHardwareId)
        }

        vm.showProgressbar.observeWithErrorHandling(viewLifecycleOwner, this::onError) { show ->
            if (show) {
                showProgress()
            } else {
                dismissProgress()
            }
        }

        vm.destination.observeWithErrorHandling(viewLifecycleOwner, this::onError) {
            findNavController().navigate(it)
        }

        vm.showErrorMessageEvent.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            SnackBarUtil.showErrorSnackBar(view, it)
        }

        setUpSignIn()
        setUpDeeplink(view)
    }

    private fun setUpSignIn() {
        email_address.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let { vm.handleAction(LoginChangedAction(it.toString())) }
            }
        })

        password.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let { vm.handleAction(PasswordChangedAction(it.toString())) }
            }
        })

        args.email?.let {
            email_address.setText(it)
        }

        sign_in.setOnClickListener {
            vm.handleAction(OnLoginClickAction)
        }

        bCopyHardwareId.setOnClickListener {
            vm.handleAction(CopyHardwareIdAction)
        }
    }

    private fun setUpDeeplink(view: View) {
        vm.clearDeeplinkTextEvent.observeWithErrorHandling(viewLifecycleOwner, vm::onError) {
            etDeeplink.setText("")
        }

        bDeeplinkIssues.setOnClickListener {
            vm.onDeeplinkIssuesClicked()
        }

        bClose.setOnClickListener {
            vm.handleAction(OnCloseClickAction)
        }

        lDeeplinkIssues.dim.setOnClickListener {
            vm.handleAction(OnCloseClickAction)
        }

        bDeeplinkLogin.setOnClickListener {
            vm.handleAction(DeeplinkOrTokenPastedAction(etDeeplink.textString(), mainActivity()))
        }
    }

    private fun onError(exception: Exception) {
        vm.handleAction(ErrorAction(exception))
    }
}
