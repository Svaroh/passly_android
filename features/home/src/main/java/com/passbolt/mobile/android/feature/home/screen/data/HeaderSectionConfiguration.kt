package net.svaroh.passly.feature.home.screen.data

data class HeaderSectionConfiguration(
    val isInCurrentFolderSectionVisible: Boolean,
    val isInSubFoldersSectionVisible: Boolean,
    val currentFolderName: String?,
    val isSuggestedSectionVisible: Boolean,
    val isOtherItemsSectionVisible: Boolean,
    val areAllSectionsEmpty: Boolean,
)
