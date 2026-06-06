package net.svaroh.passly.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HelpMenuModel(
    val shouldShowShowQrCodesHelp: Boolean,
    val shouldShowImportProfile: Boolean,
    val shouldShowImportAccountKit: Boolean,
) : Parcelable
