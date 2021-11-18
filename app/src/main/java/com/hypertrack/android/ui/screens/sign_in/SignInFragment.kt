package com.hypertrack.android.ui.screens.sign_in

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
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
        MyApplication.injector.provideViewModelFactory(MyApplication.context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpSignIn()
        setUpDeeplink(view)
    }

    private fun setUpSignIn() {
        email_address.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let { vm.onLoginTextChanged(it) }
            }
        })

        password.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let { vm.onPasswordTextChanged(it) }
            }
        })

        args.email?.let {
            email_address.setText(it)
        }

        vm.destination.observe(viewLifecycleOwner) {
            findNavController().navigate(it)
        }

        vm.loginErrorText.observe(viewLifecycleOwner, {
            incorrect.text = it
        })

        sign_in.setOnClickListener { vm.onLoginClick() }

        vm.loadingState.observe(viewLifecycleOwner) { show ->
            if (show) showProgress() else dismissProgress()
        }

        //todo rounded corners
        vm.isLoginButtonClickable.observe(viewLifecycleOwner) { isClickable ->
            sign_in.isEnabled = isClickable
            sign_in.isSelected = isClickable
        }
    }

    private fun setUpDeeplink(view: View) {
        vm.deeplinkErrorText.observe(viewLifecycleOwner) {
            tvDeeplinkError.setGoneState(it == null)
            tvDeeplinkError.text = it
        }

        vm.clearDeeplinkTextAction.observe(viewLifecycleOwner) {
            etDeeplink.setText("")
        }

        vm.showPasteDeeplink.observe(viewLifecycleOwner) { show ->
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

        bDeeplinkIssues.setOnClickListener {
            vm.onDeeplinkIssuesClick()
        }

        bClose.setOnClickListener {
            vm.onCloseClick()
        }

        lDeeplinkIssues.dim.setOnClickListener {
            vm.onCloseClick()
        }

        bDeeplinkLogin.setOnClickListener {
            vm.handleDeeplinkOrToken(etDeeplink.textString(), mainActivity())
        }
    }


}