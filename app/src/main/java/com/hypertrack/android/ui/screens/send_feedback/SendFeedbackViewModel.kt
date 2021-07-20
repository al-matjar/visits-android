package com.hypertrack.android.ui.screens.send_feedback

import android.app.Activity
import com.hypertrack.android.interactors.FeedbackInteractor
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R

class SendFeedbackViewModel(
    private val feedbackInteractor: FeedbackInteractor,
    private val osUtilsProvider: OsUtilsProvider
) : BaseViewModel(osUtilsProvider) {

    fun getSavedText(): String {
        return feedbackInteractor.savedFeedbackText
    }

    fun onSendClick(activity: Activity, text: String) {
        feedbackInteractor.sendFeedback(activity, text)
        osUtilsProvider.makeToast(R.string.profile_report_sent)
        popBackStack.postValue(true)
    }

    fun onPause(text: String) {
        feedbackInteractor.savedFeedbackText = text
    }


}