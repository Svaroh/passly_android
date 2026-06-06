package net.svaroh.passly.core.ui.slider

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import net.svaroh.passly.core.ui.screenshot.PassboltPreviewWrapper

private const val MIN_PASSWORD_LENGTH = 8
private const val MAX_PASSWORD_LENGTH = 128

private val passwordLength = MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun LabelledSliderMinimumScreenshot() {
    LabelledSlider(
        title = "Length",
        value = passwordLength.first,
        valueRange = passwordLength,
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun LabelledSliderMidRangeScreenshot() {
    LabelledSlider(
        title = "Length",
        value = 40,
        valueRange = passwordLength,
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
    )
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun LabelledSliderMaximumScreenshot() {
    LabelledSlider(
        title = "Length",
        value = passwordLength.last,
        valueRange = passwordLength,
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun LabelledSliderDarkThemeScreenshot() {
    LabelledSlider(
        title = "Length",
        value = 40,
        valueRange = passwordLength,
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
    )
}
