package com.hypertrack.android.ui.screens.send_feedback

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.hypertrack.android.ui.MainActivity
import com.hypertrack.android.ui.base.BaseFragment
import com.hypertrack.android.ui.common.util.textString
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.stringFromResource
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_send_feedback.*
import kotlinx.android.synthetic.main.fragment_send_feedback.toolbar

class SendFeedbackFragment : BaseFragment<MainActivity>(R.layout.fragment_send_feedback) {

    private val vm: SendFeedbackViewModel by viewModels {
        MyApplication.injector.provideUserScopeViewModelFactory()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.title = R.string.feedback_send.stringFromResource()
        mainActivity().setSupportActionBar(toolbar)
        mainActivity().supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

        vm.popBackStack.observe(viewLifecycleOwner, {
            findNavController().popBackStack()
        })

        if (etFeedback.textString().isBlank()) {
            etFeedback.setText(vm.getSavedText())
        }

        bSend.setOnClickListener {
            vm.onSendClick(requireActivity(), etFeedback.textString())
        }
    }

    override fun onPause() {
        super.onPause()
        vm.onPause(etFeedback.textString())
    }
}