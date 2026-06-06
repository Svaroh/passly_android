package net.svaroh.passly.metadata.usecase

class MetadataSettingsFileName(
    userId: String,
) {
    val name = METADATA_SETTINGS_FILE_NAME_FORMAT.format(userId)

    private companion object {
        private const val METADATA_SETTINGS_FILE_NAME = "metadata_settings"
        private const val METADATA_SETTINGS_FILE_NAME_FORMAT = "${METADATA_SETTINGS_FILE_NAME}_%s"
    }
}
