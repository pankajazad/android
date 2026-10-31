package org.cryptomator.presentation.ui.activity

import android.content.Intent
import org.cryptomator.generator.Activity
import org.cryptomator.generator.InjectIntent
import org.cryptomator.presentation.databinding.ActivityLicenseCheckBinding
import org.cryptomator.presentation.intent.LicenseCheckIntent

/** Keeps existing intents and activation links compatible with this license-free fork. */
@Activity
class LicenseCheckActivity : BaseActivity<ActivityLicenseCheckBinding>(ActivityLicenseCheckBinding::inflate) {

	@InjectIntent
	lateinit var licenseCheckIntent: LicenseCheckIntent

	override fun setupView() {
		setResult(RESULT_OK)
		finish()
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		setResult(RESULT_OK)
		finish()
	}
}
