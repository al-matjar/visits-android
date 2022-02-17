package com.hypertrack.android.ui.screens.splash_screen

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.hypertrack.android.di.Injector
import com.hypertrack.android.interactors.app.SplashScreenOpenedAction
import com.hypertrack.android.ui.base.ProgressDialogFragment
import com.hypertrack.android.ui.base.navigate
import com.hypertrack.android.ui.common.util.SnackbarUtil
import com.hypertrack.logistics.android.github.R

@SuppressLint("CustomSplashScreen")
class SplashScreenFragment : ProgressDialogFragment(R.layout.fragment_splash_screen) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Injector.provideAppInteractor().handleAction(SplashScreenOpenedAction)
    }
}
