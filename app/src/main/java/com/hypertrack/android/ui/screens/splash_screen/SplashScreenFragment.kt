package com.hypertrack.android.ui.screens.splash_screen

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import com.hypertrack.android.di.Injector
import com.hypertrack.android.interactors.app.AppErrorAction
import com.hypertrack.android.interactors.app.RegisterScreenAction
import com.hypertrack.android.interactors.app.state.SplashScreen
import com.hypertrack.android.ui.base.ProgressDialogFragment
import com.hypertrack.logistics.android.github.R
import kotlinx.android.synthetic.main.fragment_splash_screen.ivLogo

@SuppressLint("CustomSplashScreen")
class SplashScreenFragment : ProgressDialogFragment(R.layout.fragment_splash_screen) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Injector.provideAppInteractor().handleAction(RegisterScreenAction(SplashScreen))

        ivLogo.setOnClickListener {
            Injector.provideAppInteractor()
                .handleAction(AppErrorAction(FreezeOnSplashScreenReported()))
        }
    }
}
