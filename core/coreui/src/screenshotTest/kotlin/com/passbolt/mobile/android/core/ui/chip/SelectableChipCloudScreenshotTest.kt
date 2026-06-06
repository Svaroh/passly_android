package net.svaroh.passly.core.ui.chip

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import net.svaroh.passly.core.ui.screenshot.PassboltPreviewWrapper

private val characterGroups =
    listOf(
        SelectableChipItemModel("upper", "A-Z", isSelected = true),
        SelectableChipItemModel("digit", "0-9", isSelected = true),
        SelectableChipItemModel("lower", "a-z", isSelected = true),
        SelectableChipItemModel("special1", "# $ % &", isSelected = true),
        SelectableChipItemModel("parenthesis", "{ [ ( | ) ] }", isSelected = false),
        SelectableChipItemModel("punctuation", "< > ? /", isSelected = false),
    )

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun SelectableChipCloudScreenshot() {
    SelectableChipCloud(
        items = characterGroups,
        onToggle = {},
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun SelectableChipCloudDarkThemeScreenshot() {
    SelectableChipCloud(
        items = characterGroups,
        onToggle = {},
    )
}

@PreviewTest
@Preview(showBackground = true, fontScale = 1.5f)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun SelectableChipCloudLargeFontScreenshot() {
    SelectableChipCloud(
        items = characterGroups,
        onToggle = {},
    )
}
