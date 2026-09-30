package org.cryptomator.presentation.util

object FileNameSearch {

	fun matches(fileName: String, query: String): Boolean {
		return query.isEmpty() || fileName.contains(query, ignoreCase = true)
	}
}
