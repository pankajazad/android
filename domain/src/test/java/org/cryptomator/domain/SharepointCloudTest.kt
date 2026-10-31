package org.cryptomator.domain

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class SharepointCloudTest {

	private val cloud = SharepointCloud.aSharepointCloud() //
		.withId(42L) //
		.withAccessToken("accessToken") //
		.withUsername("pankajhere@outlook.in") //
		.withSiteUrl("https://contoso.sharepoint.com/sites/team") //
		.withDriveId("driveId") //
		.withDriveName("Documents") //
		.build()

	@Test
	fun testCopyKeepsAllFields() {
		val copy = SharepointCloud.aCopyOf(cloud).build()

		assertThat(copy.id(), `is`(42L))
		assertThat(copy.accessToken(), `is`("accessToken"))
		assertThat(copy.username(), `is`("pankajhere@outlook.in"))
		assertThat(copy.siteUrl(), `is`("https://contoso.sharepoint.com/sites/team"))
		assertThat(copy.driveId(), `is`("driveId"))
		assertThat(copy.driveName(), `is`("Documents"))
	}

	@Test
	fun testConfigurationMatchesSameUserAndDrive() {
		val sameDrive = SharepointCloud.aSharepointCloud() //
			.withUsername("pankajhere@outlook.in") //
			.withDriveId("driveId") //
			.build()

		assertThat(cloud.configurationMatches(sameDrive), `is`(true))
	}

	@Test
	fun testConfigurationDoesNotMatchOtherDriveOfSameUser() {
		val otherDrive = SharepointCloud.aCopyOf(cloud).withId(null).withDriveId("otherDriveId").build()

		assertThat(cloud.configurationMatches(otherDrive), `is`(false))
	}

	@Test
	fun testConfigurationDoesNotMatchSameDriveOfOtherUser() {
		val otherUser = SharepointCloud.aCopyOf(cloud).withId(null).withUsername("other-user").build()

		assertThat(cloud.configurationMatches(otherUser), `is`(false))
	}

	@Test
	fun testConfigurationDoesNotMatchOnedriveCloudOfSameUser() {
		val onedriveCloud = OnedriveCloud.aOnedriveCloud().withUsername("pankajhere@outlook.in").build()

		assertThat(cloud.configurationMatches(onedriveCloud), `is`(false))
	}
}
