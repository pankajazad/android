package org.cryptomator.presentation.ui.activity

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import org.cryptomator.generator.Activity
import org.cryptomator.presentation.R
import org.cryptomator.presentation.databinding.ActivityWelcomeBinding
import org.cryptomator.presentation.presenter.WelcomePresenter
import org.cryptomator.presentation.service.ProductPrices
import org.cryptomator.presentation.ui.activity.view.WelcomeView
import org.cryptomator.presentation.ui.fragment.WelcomeIntroFragment
import org.cryptomator.presentation.ui.fragment.WelcomeNotificationsFragment
import org.cryptomator.presentation.ui.fragment.WelcomeScreenLockFragment
import org.cryptomator.presentation.ui.layout.ObscuredAwareCoordinatorLayout
import javax.inject.Inject

@Activity
class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>(ActivityWelcomeBinding::inflate), //
	WelcomeView, //
	WelcomeNotificationsFragment.Listener, //
	WelcomeScreenLockFragment.Listener {

	@Inject
	lateinit var welcomePresenter: WelcomePresenter

	private val keyguardManager by lazy { getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }

	private lateinit var pagerAdapter: WelcomePagerAdapter
	private val pages = mutableListOf<FragmentPage>()
	private var navBasePaddingBottom: Int = 0

	override fun setupView() {
		if (sharedPreferencesHandler.hasCompletedWelcomeFlow()) {
			openVaultList()
			return
		}

		setSupportActionBar(binding.mtToolbar.toolbar)
		supportActionBar?.title = getString(R.string.screen_welcome_title)
		supportActionBar?.setDisplayHomeAsUpEnabled(false)
		binding.mtToolbar.toolbar.navigationIcon = null

		binding.activityRootView.setOnFilteredTouchEventForSecurityListener(object : ObscuredAwareCoordinatorLayout.Listener {
			override fun onFilteredTouchEventForSecurity() {
				welcomePresenter.onFilteredTouchEventForSecurity()
			}
		})
		navBasePaddingBottom = binding.navigationContainer.paddingBottom
		ViewCompat.setOnApplyWindowInsetsListener(binding.navigationContainer) { view, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
			val extra = (8 * resources.displayMetrics.density).toInt()
			view.updatePadding(bottom = navBasePaddingBottom + systemBars.bottom + extra)
			insets
		}

		setupPages()
		setupPager()
		setupBackPressedCallback()

		updateNotificationPermissionState()
		updateScreenLockState()
	}

	override fun onResume() {
		super.onResume()
		if (sharedPreferencesHandler.hasCompletedWelcomeFlow() && !isFinishing) {
			openVaultList()
			return
		}
		updateNotificationPermissionState()
		updateScreenLockState()
	}

	private val backPressedCallback = object : OnBackPressedCallback(true) {
		override fun handleOnBackPressed() {
			goBackOrExit()
		}
	}

	private fun setupBackPressedCallback() {
		onBackPressedDispatcher.addCallback(this, backPressedCallback)
	}

	private fun setupPages() {
		pages.clear()
		pages.add(FragmentPage.Intro)
		pages.add(FragmentPage.Notifications)
		if (!keyguardManager.isDeviceSecure) {
			pages.add(FragmentPage.ScreenLock)
		}
	}

	private fun setupPager() {
		pagerAdapter = WelcomePagerAdapter(this, pages)
		binding.welcomePager.adapter = pagerAdapter
		binding.welcomePager.setCurrentItem(0, false)
		binding.welcomePager.isUserInputEnabled = true
		binding.welcomePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
			override fun onPageSelected(position: Int) {
				updateNavigationButtons(position)
				when (pages[position]) {
					FragmentPage.Notifications -> updateNotificationPermissionState()
					FragmentPage.ScreenLock -> updateScreenLockState()
					FragmentPage.Intro -> Unit
				}
			}
		})
		updateNavigationButtons(0)
		binding.btnBack.setOnClickListener {
			goBackOrExit()
		}
		binding.btnNext.setOnClickListener {
			advanceOrComplete()
		}
	}

	private fun updateNavigationButtons(position: Int) {
		binding.btnBack.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
		binding.btnNext.text = if (position == pagerAdapter.itemCount - 1) {
			getString(R.string.screen_welcome_continue_button)
		} else {
			getString(R.string.next)
		}
	}

	private fun hasNotificationPermission(): Boolean {
		return Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 || ContextCompat.checkSelfPermission(
			this,
			Manifest.permission.POST_NOTIFICATIONS
		) == PackageManager.PERMISSION_GRANTED
	}

	private fun updateNotificationPermissionState(grantedOverride: Boolean? = null) {
		if (!this::pagerAdapter.isInitialized) {
			return
		}
		val granted = grantedOverride ?: hasNotificationPermission()
		pagerAdapter.notificationsFragment?.updatePermissionState(granted)
	}

	private fun updateScreenLockState() {
		if (!this::pagerAdapter.isInitialized) {
			return
		}
		pagerAdapter.screenLockFragment?.updateScreenLockState(keyguardManager.isDeviceSecure)
	}

	private fun completeWelcomeFlow() {
		sharedPreferencesHandler.setWelcomeFlowCompleted()
		sharedPreferencesHandler.setScreenLockDialogAlreadyShown()
		openVaultList()
	}

	private fun openVaultList() {
		setResult(RESULT_OK)
		finish()
	}

	// License callbacks remain part of WelcomeView for compatibility with its presenter.
	override fun showOrUpdateLicenseEntry(license: String) = Unit

	override fun showConfirmationDialog(mail: String) = Unit

	override fun bindProductPrices(prices: ProductPrices) = Unit

	override fun hasLicenseView(): Boolean = false

	override fun onNotificationPermissionResult(granted: Boolean) {
		updateNotificationPermissionState(granted)
	}

	// WelcomeNotificationsFragment.Listener

	override fun onRequestNotifications() {
		welcomePresenter.requestNotificationPermission()
	}

	// WelcomeScreenLockFragment.Listener

	override fun onSetScreenLock(setScreenLock: Boolean) {
		welcomePresenter.onSetScreenLock(setScreenLock)
	}

	private fun goBackOrExit() {
		val pos = binding.welcomePager.currentItem
		if (pos > 0) {
			binding.welcomePager.currentItem = pos - 1
		} else {
			finishAffinity()
		}
	}

	private fun advanceOrComplete() {
		val pos = binding.welcomePager.currentItem
		if (pos < pagerAdapter.itemCount - 1) {
			binding.welcomePager.currentItem = pos + 1
		} else {
			completeWelcomeFlow()
		}
	}

	// Distinct IDs prevent an in-progress welcome flow from restoring the old license page after an update.
	private enum class FragmentPage(val itemId: Long) { Intro(100), Notifications(101), ScreenLock(102) }

	private inner class WelcomePagerAdapter(activity: AppCompatActivity, private val pages: List<FragmentPage>) : androidx.viewpager2.adapter.FragmentStateAdapter(activity) {

		val notificationsFragment: WelcomeNotificationsFragment?
			get() = findPageFragment(FragmentPage.Notifications)

		val screenLockFragment: WelcomeScreenLockFragment?
			get() = findPageFragment(FragmentPage.ScreenLock)

		private inline fun <reified F : Fragment> findPageFragment(page: FragmentPage): F? {
			val pos = pages.indexOf(page)
			return if (pos >= 0) supportFragmentManager.findFragmentByTag("f${getItemId(pos)}") as? F else null
		}

		override fun getItemCount(): Int = pages.size

		override fun getItemId(position: Int): Long = pages[position].itemId

		override fun containsItem(itemId: Long): Boolean = pages.any { it.itemId == itemId }

		override fun createFragment(position: Int): Fragment = when (pages[position]) {
			FragmentPage.Intro -> WelcomeIntroFragment()
			FragmentPage.Notifications -> WelcomeNotificationsFragment()
			FragmentPage.ScreenLock -> WelcomeScreenLockFragment()
		}
	}
}
