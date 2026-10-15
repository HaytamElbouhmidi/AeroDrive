package com.haytam.aerodrive.car

import android.content.Intent
import android.content.res.Configuration
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.haytam.aerodrive.BuildConfig

class AeroDriveCarService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        check(BuildConfig.DEBUG) { "Use a validated host allowlist before enabling release builds." }
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }
    override fun onCreateSession(): Session = object : Session() {
        private var screen: AustinScreen? = null
        override fun onCreateScreen(intent: Intent): Screen = AustinScreen(carContext).also { screen = it }
        override fun onCarConfigurationChanged(newConfiguration: Configuration) {
            super.onCarConfigurationChanged(newConfiguration)
            screen?.configurationChanged(newConfiguration)
        }
    }
}
