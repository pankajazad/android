package org.cryptomator.presentation.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FileNameSearchTest {

	@Test
	fun `matches every file name for an empty query`() {
		assertTrue(FileNameSearch.matches("Quarterly Report.pdf", ""))
	}

	@Test
	fun `matches a file name substring regardless of case`() {
		assertTrue(FileNameSearch.matches("Quarterly Report.pdf", "report"))
	}

	@Test
	fun `treats special characters in a query as literal text`() {
		assertTrue(FileNameSearch.matches("Budget [Final].xlsx", "[final]"))
	}

	@Test
	fun `does not match a missing file name substring`() {
		assertFalse(FileNameSearch.matches("Quarterly Report.pdf", "invoice"))
	}
}
