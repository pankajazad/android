package org.cryptomator.presentation.presenter

import android.app.Activity
import org.cryptomator.data.util.NetworkConnectionCheck
import org.cryptomator.domain.usecases.DoUpdateCheckUseCase
import org.cryptomator.domain.usecases.DoUpdateUseCase
import org.cryptomator.domain.usecases.GetDecryptedCloudForVaultUseCase
import org.cryptomator.domain.usecases.cloud.GetRootFolderUseCase
import org.cryptomator.domain.usecases.vault.DeleteVaultUseCase
import org.cryptomator.domain.usecases.vault.GetVaultListUseCase
import org.cryptomator.domain.usecases.vault.ListCBCEncryptedPasswordVaultsUseCase
import org.cryptomator.domain.usecases.vault.LockVaultUseCase
import org.cryptomator.domain.usecases.vault.MoveVaultPositionUseCase
import org.cryptomator.domain.usecases.vault.RemoveStoredVaultPasswordsUseCase
import org.cryptomator.domain.usecases.vault.RenameVaultUseCase
import org.cryptomator.domain.usecases.vault.SaveVaultUseCase
import org.cryptomator.domain.usecases.vault.SaveVaultsUseCase
import org.cryptomator.presentation.exception.ExceptionHandlers
import org.cryptomator.presentation.licensing.LicenseEnforcer
import org.cryptomator.presentation.model.mappers.CloudFolderModelMapper
import org.cryptomator.presentation.service.ProductPricesCache
import org.cryptomator.presentation.service.SalePromo
import org.cryptomator.presentation.ui.activity.view.VaultListView
import org.cryptomator.presentation.util.FileUtil
import org.cryptomator.presentation.workflow.AddExistingVaultWorkflow
import org.cryptomator.presentation.workflow.AuthenticationExceptionHandler
import org.cryptomator.presentation.workflow.CreateNewVaultWorkflow
import org.cryptomator.util.SharedPreferencesHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any

class VaultListPresenterFreemiumTest {

	private val vaultListView: VaultListView = Mockito.mock(VaultListView::class.java)
	private val activity: Activity = Mockito.mock(Activity::class.java)
	private val licenseEnforcer: LicenseEnforcer = Mockito.mock(LicenseEnforcer::class.java)
	private val sharedPreferencesHandler: SharedPreferencesHandler = Mockito.mock(SharedPreferencesHandler::class.java)
	private val productPricesCache: ProductPricesCache = Mockito.mock(ProductPricesCache::class.java)
	private val exceptionMappings: ExceptionHandlers = Mockito.mock(ExceptionHandlers::class.java)

	private lateinit var inTest: VaultListPresenter

	@BeforeEach
	fun setup() {
		inTest = VaultListPresenter(
			Mockito.mock(GetVaultListUseCase::class.java),
			Mockito.mock(DeleteVaultUseCase::class.java),
			Mockito.mock(RenameVaultUseCase::class.java),
			Mockito.mock(LockVaultUseCase::class.java),
			Mockito.mock(GetDecryptedCloudForVaultUseCase::class.java),
			Mockito.mock(GetRootFolderUseCase::class.java),
			Mockito.mock(AddExistingVaultWorkflow::class.java),
			Mockito.mock(CreateNewVaultWorkflow::class.java),
			Mockito.mock(SaveVaultUseCase::class.java),
			Mockito.mock(MoveVaultPositionUseCase::class.java),
			Mockito.mock(DoUpdateCheckUseCase::class.java),
			Mockito.mock(DoUpdateUseCase::class.java),
			Mockito.mock(ListCBCEncryptedPasswordVaultsUseCase::class.java),
			Mockito.mock(RemoveStoredVaultPasswordsUseCase::class.java),
			Mockito.mock(SaveVaultsUseCase::class.java),
			Mockito.mock(NetworkConnectionCheck::class.java),
			Mockito.mock(FileUtil::class.java),
			Mockito.mock(AuthenticationExceptionHandler::class.java),
			Mockito.mock(CloudFolderModelMapper::class.java),
			licenseEnforcer,
			sharedPreferencesHandler,
			productPricesCache,
			exceptionMappings
		)
		Mockito.doReturn(activity).`when`(vaultListView).activity()
		Mockito.doAnswer { invocation ->
			invocation.getArgument<Runnable>(0).run()
		}.`when`(activity).runOnUiThread(any())
		Mockito.doReturn(true).`when`(sharedPreferencesHandler).hasCompletedWelcomeFlow()
		inTest.view = vaultListView
	}

	@Test
	fun `resumed provides the app without a license or a trial and does not load purchase offers`() {
		Mockito.doReturn("").`when`(sharedPreferencesHandler).licenseToken()
		Mockito.doReturn(false).`when`(sharedPreferencesHandler).hasRunningSubscription()
		Mockito.doReturn(0L).`when`(sharedPreferencesHandler).trialExpirationDate()

		inTest.resumed()

		Mockito.verify(vaultListView).hideSalePromoBanner()
		Mockito.verify(vaultListView, Mockito.never()).showDialog(any())
		Mockito.verify(productPricesCache, Mockito.never()).queryProductPrices(any())
		Mockito.verify(sharedPreferencesHandler, Mockito.never()).addLicenseChangedListeners(any())
	}

	@Test
	fun `resumed ignores an expired trial without a paid license`() {
		Mockito.doReturn(false).`when`(licenseEnforcer).hasPaidLicense()
		Mockito.doReturn(true).`when`(sharedPreferencesHandler).isTrialExpired()
		Mockito.doReturn(LicenseEnforcer.TrialState(false, true, "Mar 28, 2026")).`when`(licenseEnforcer).evaluateTrialState()

		inTest.resumed()

		Mockito.verify(vaultListView, Mockito.never()).showDialog(any())
		Mockito.verify(licenseEnforcer, Mockito.never()).evaluateTrialState()
		Mockito.verify(productPricesCache, Mockito.never()).queryProductPrices(any())
	}

	@Test
	fun `revoking a saved purchase between resumes does not show a paywall`() {
		Mockito.doReturn("saved-token", "").`when`(sharedPreferencesHandler).licenseToken()
		Mockito.doReturn(true, false).`when`(sharedPreferencesHandler).hasRunningSubscription()

		inTest.resumed()
		inTest.pause()
		inTest.resumed()

		Mockito.verify(vaultListView, Mockito.times(2)).hideSalePromoBanner()
		Mockito.verify(vaultListView, Mockito.never()).showDialog(any())
		Mockito.verify(vaultListView, Mockito.never()).showSalePromoBanner(any())
		Mockito.verify(productPricesCache, Mockito.never()).queryProductPrices(any())
	}

	@Test
	fun `a stale unlock full version action does not launch a purchase screen`() {
		inTest.onUnlockFullVersionClicked()

		Mockito.verify(activity, Mockito.never()).startActivity(any())
		Mockito.verify(vaultListView, Mockito.never()).showDialog(any())
		Mockito.verify(vaultListView).hideSalePromoBanner()
	}

	@Test
	fun `onSalePromoBannerDismissed remembers sale end and hides banner`() {
		inTest.onSalePromoBannerDismissed(SalePromo(33, 1_700_000_000_000))

		Mockito.verify(sharedPreferencesHandler).setSalePromoDismissedUntil(1_700_000_000_000)
		Mockito.verify(vaultListView).hideSalePromoBanner()
	}
}
