package org.cryptomator.presentation.licensing

import android.app.Activity
import org.cryptomator.presentation.model.VaultModel
import org.cryptomator.util.FlavorConfig
import org.cryptomator.util.SharedPreferencesHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class LicenseEnforcerTest {

	private val sharedPreferencesHandler: SharedPreferencesHandler = mock()
	private lateinit var licenseEnforcer: LicenseEnforcer

	@BeforeEach
	fun setUp() {
		licenseEnforcer = LicenseEnforcer(sharedPreferencesHandler)
	}

	@Test
	fun `every local action is available without a license and never opens a purchase screen`() {
		val activity: Activity = mock()

		LicenseEnforcer.LockedAction.entries.forEach { action ->
			assertTrue(licenseEnforcer.ensureWriteAccess(activity, action), action.name)
			assertTrue(licenseEnforcer.ensureWriteAccessForVault(activity, null, action), action.name)
		}

		verifyNoInteractions(activity, sharedPreferencesHandler)
	}

	@Test
	fun `expired trial and revoked purchases cannot remove write access`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() - 1000L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(true)

		assertTrue(licenseEnforcer.hasWriteAccess())
		assertTrue(licenseEnforcer.hasWriteAccessForVault(null))
		assertTrue(licenseEnforcer.peekWriteAccessForVault(null))
		verifyNoInteractions(sharedPreferencesHandler)
	}

	@Test
	fun `Hub vault access requires no local or Hub purchase and opens no purchase activity`() {
		val activity: Activity = mock()
		val vault: VaultModel = mock()
		`when`(vault.isHubVault).thenReturn(true)
		`when`(vault.hasHubPaidLicense).thenReturn(false)

		assertTrue(licenseEnforcer.hasWriteAccessForVault(vault))
		assertTrue(licenseEnforcer.peekWriteAccessForVault(vault))
		LicenseEnforcer.LockedAction.entries.forEach { action ->
			assertTrue(licenseEnforcer.ensureWriteAccessForVault(activity, vault, action), action.name)
		}
		verifyNoInteractions(activity, sharedPreferencesHandler)
	}

	// -- hasPaidLicense --

	@Test
	fun `hasPaidLicense returns true when license token is present`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("some-token")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)

		assertTrue(licenseEnforcer.hasPaidLicense())
	}

	@Test
	fun `hasPaidLicense returns true when subscription is active`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(true)

		assertTrue(licenseEnforcer.hasPaidLicense())
	}

	@Test
	fun `trial is not recorded as a paid purchase`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() + 86400000L)

		assertEquals(FlavorConfig.isPremiumFlavor, licenseEnforcer.hasPaidLicense())
	}

	@Test
	fun `missing purchases do not become paid licenses`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)

		assertEquals(FlavorConfig.isPremiumFlavor, licenseEnforcer.hasPaidLicense())
	}

	// -- hasActiveTrial --

	@Test
	fun `hasActiveTrial returns true when trial expiration is in the future`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() + 86400000L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(false)

		assertTrue(licenseEnforcer.hasActiveTrial())
	}

	@Test
	fun `hasActiveTrial returns false when trial expiration is in the past`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() - 1000L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(false)

		assertFalse(licenseEnforcer.hasActiveTrial())
	}

	@Test
	fun `hasActiveTrial returns false when no trial was started`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)

		assertFalse(licenseEnforcer.hasActiveTrial())
	}

	// -- startTrial --

	@Test
	fun `startTrial sets trial expiration date 30 days in the future`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)
		val before = System.currentTimeMillis()
		licenseEnforcer.startTrial()
		val after = System.currentTimeMillis()

		val captor = ArgumentCaptor.forClass(Long::class.java)
		verify(sharedPreferencesHandler).setTrialExpirationDate(captor.capture())

		val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
		assertTrue(captor.value >= before + thirtyDaysMs)
		assertTrue(captor.value <= after + thirtyDaysMs)
	}

	@Test
	fun `startTrial does not overwrite existing active trial`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() + 86400000L)

		licenseEnforcer.startTrial()

		verify(sharedPreferencesHandler, never()).setTrialExpirationDate(anyLong())
	}

	@Test
	fun `startTrial does not overwrite expired trial`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() - 1000L)

		licenseEnforcer.startTrial()

		verify(sharedPreferencesHandler, never()).setTrialExpirationDate(anyLong())
	}

	// -- evaluateTrialState --

	@Test
	fun `evaluateTrialState returns active with formatted date when trial is active`() {
		val futureDate = System.currentTimeMillis() + 86400000L
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(futureDate)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(false)

		val state = licenseEnforcer.evaluateTrialState()

		assertTrue(state.isActive)
		assertFalse(state.isExpired)
		assertNotNull(state.formattedExpirationDate)
	}

	@Test
	fun `evaluateTrialState returns expired with formatted date when trial is expired`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() - 1000L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(false)

		val state = licenseEnforcer.evaluateTrialState()

		assertFalse(state.isActive)
		assertTrue(state.isExpired)
		assertNotNull(state.formattedExpirationDate)
	}

	@Test
	fun `evaluateTrialState returns inactive and not expired when no trial started`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)

		val state = licenseEnforcer.evaluateTrialState()

		assertFalse(state.isActive)
		assertFalse(state.isExpired)
		assertNull(state.formattedExpirationDate)
	}

	// -- sticky trial expiry --

	@Test
	fun `hasActiveTrial latches isTrialExpired when expiration is in the past`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() - 1000L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(false)

		assertFalse(licenseEnforcer.hasActiveTrial())
		verify(sharedPreferencesHandler).setTrialExpired(true)
	}

	@Test
	fun `hasActiveTrial returns false when isTrialExpired is already true even if date is in the future`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() + 86400000L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(true)

		assertFalse(licenseEnforcer.hasActiveTrial())
		verify(sharedPreferencesHandler, never()).setTrialExpired(anyBoolean())
	}

	@Test
	fun `evaluateTrialState latches isTrialExpired and returns expired when date is in the past`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() - 1000L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(false)

		val state = licenseEnforcer.evaluateTrialState()

		assertFalse(state.isActive)
		assertTrue(state.isExpired)
		verify(sharedPreferencesHandler).setTrialExpired(true)
	}

	@Test
	fun `evaluateTrialState returns expired when isTrialExpired is already true even if date is in the future`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() + 86400000L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(true)

		val state = licenseEnforcer.evaluateTrialState()

		assertFalse(state.isActive)
		assertTrue(state.isExpired)
		assertNotNull(state.formattedExpirationDate)
		verify(sharedPreferencesHandler, never()).setTrialExpired(anyBoolean())
	}

	@Test
	fun `evaluateTrialState is idempotent once sticky flag is set`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() - 1000L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(true)

		licenseEnforcer.hasActiveTrial()
		licenseEnforcer.evaluateTrialState()

		verify(sharedPreferencesHandler, never()).setTrialExpired(anyBoolean())
	}

	@Test
	fun `startTrial does not reset sticky trialExpired flag`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(true)

		licenseEnforcer.startTrial()

		verify(sharedPreferencesHandler, never()).setTrialExpired(anyBoolean())
	}

	@Test
	fun `fresh install with no trial and no flag behaves unchanged`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(false)

		assertFalse(licenseEnforcer.hasActiveTrial())
		val state = licenseEnforcer.evaluateTrialState()
		assertFalse(state.isActive)
		assertFalse(state.isExpired)
		assertNull(state.formattedExpirationDate)
		verify(sharedPreferencesHandler, never()).setTrialExpired(anyBoolean())
	}

	// -- evaluateUiState --

	@Test
	fun `evaluateUiState returns active trial with expiration text`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() + 86400000L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(false)

		val uiState = licenseEnforcer.evaluateUiState()

		assertTrue(uiState.hasWriteAccess)
		assertEquals(FlavorConfig.isPremiumFlavor, uiState.hasPaidLicense)
		assertTrue(uiState.trialState.isActive)
		assertNotNull(uiState.trialState.formattedExpirationDate)
	}

	@Test
	fun `expired trial metadata does not disable write access`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() - 1000L)
		`when`(sharedPreferencesHandler.isTrialExpired()).thenReturn(false)

		val uiState = licenseEnforcer.evaluateUiState()

		assertTrue(uiState.hasWriteAccess)
		assertEquals(FlavorConfig.isPremiumFlavor, uiState.hasPaidLicense)
		assertTrue(uiState.trialState.isExpired)
		assertNotNull(uiState.trialState.formattedExpirationDate)
	}

	@Test
	fun `fresh install UI has write access without a license or trial`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)

		val uiState = licenseEnforcer.evaluateUiState()

		assertTrue(uiState.hasWriteAccess)
		assertEquals(FlavorConfig.isPremiumFlavor, uiState.hasPaidLicense)
		assertFalse(uiState.trialState.isActive)
		assertFalse(uiState.trialState.isExpired)
		assertNull(uiState.trialState.formattedExpirationDate)
	}
}
