package net.svaroh.passly.ui

data class ResourcePickerListItem(
    val resourceModel: ResourceUiModel,
    val selection: Selection,
) {
    val isSelectable = selection == Selection.SELECTABLE

    enum class Selection {
        SELECTABLE,
        NOT_SELECTABLE_NO_PERMISSION,
        NOT_SELECTABLE_UNSUPPORTED_RESOURCE_TYPE,
    }
}
