package org.cryptomator.presentation.ui.dialog

import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import org.cryptomator.generator.Dialog
import org.cryptomator.presentation.R
import org.cryptomator.presentation.databinding.DialogRecursiveImportBinding

@Dialog
class RecursiveImportProgressDialog : BaseDialog<RecursiveImportProgressDialog.Callback, DialogRecursiveImportBinding>(DialogRecursiveImportBinding::inflate) {

	interface Callback {
		fun onRecursiveImportCanceled()
	}

	private var bindingReady = false
	private var pendingUpdate: ImportProgress? = null

	private data class ImportProgress(
		val currentPath: String,
		val completedItems: Int,
		val totalItems: Int,
		val currentItemProgress: Int?
	)

	override fun setupDialog(builder: AlertDialog.Builder): android.app.Dialog {
		return builder //
			.setTitle(R.string.dialog_recursive_import_title) //
			.setNeutralButton(R.string.dialog_recursive_import_cancel_button) { _: DialogInterface?, _: Int -> } //
			.create()
	}

	override fun setupView() {
		isCancelable = false
		bindingReady = true
		val args = requireArguments()
		updateProgress(
			args.getString(ARG_CURRENT_PATH).orEmpty(),
			args.getInt(ARG_COMPLETED_ITEMS),
			args.getInt(ARG_TOTAL_ITEMS),
			args.getInt(ARG_CURRENT_ITEM_PROGRESS, -1).takeIf { it >= 0 }
		)
		pendingUpdate?.let {
			updateProgress(it.currentPath, it.completedItems, it.totalItems, it.currentItemProgress)
			pendingUpdate = null
		}
	}

	override fun onStart() {
		super.onStart()
		allowClosingDialog(false)
		enableOrientationChange(false)
		(dialog as? AlertDialog)?.let { alertDialog ->
			alertDialog.getButton(android.app.Dialog.BUTTON_NEUTRAL).setOnClickListener {
				callback?.onRecursiveImportCanceled()
				it.isEnabled = false
			}
		}
	}

	fun updateProgress(currentPath: String, completedItems: Int, totalItems: Int, currentItemProgress: Int?) {
		val update = ImportProgress(currentPath, completedItems, totalItems, currentItemProgress)
		if (!bindingReady) {
			pendingUpdate = update
			return
		}
		binding.fileCount.text = if (totalItems <= 0) {
			getString(R.string.dialog_recursive_import_scanning)
		} else {
			getString(R.string.dialog_recursive_import_items, completedItems, totalItems)
		}
		binding.currentPath.text = currentPath
		binding.llDialogIntermediateProgress.tvProgress.text = getString(R.string.dialog_recursive_import_progress)
		val progressBar = binding.llDialogIntermediateProgress.pbDialog
		progressBar.isIndeterminate = totalItems <= 0
		if (totalItems > 0) {
			val currentProgress = (currentItemProgress ?: 0).coerceIn(0, 100)
			progressBar.progress = ((completedItems * 100 + currentProgress) / totalItems).coerceIn(0, 100)
		}
	}

	companion object {
		private const val ARG_CURRENT_PATH = "currentPath"
		private const val ARG_COMPLETED_ITEMS = "completedItems"
		private const val ARG_TOTAL_ITEMS = "totalItems"
		private const val ARG_CURRENT_ITEM_PROGRESS = "currentItemProgress"

		fun newInstance(currentPath: String, completedItems: Int, totalItems: Int, currentItemProgress: Int?): RecursiveImportProgressDialog {
			return RecursiveImportProgressDialog().apply {
				arguments = Bundle().apply {
					putString(ARG_CURRENT_PATH, currentPath)
					putInt(ARG_COMPLETED_ITEMS, completedItems)
					putInt(ARG_TOTAL_ITEMS, totalItems)
					putInt(ARG_CURRENT_ITEM_PROGRESS, currentItemProgress ?: -1)
				}
			}
		}
	}
}
