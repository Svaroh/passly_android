package net.svaroh.passly.core.ui.section

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import net.svaroh.passly.core.ui.screenshot.PassboltPreviewWrapper

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun SectionScreenshot() {
    Section(title = "Shared with") {
        Text(
            text = "Content inside the section",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun SectionWithoutTitleScreenshot() {
    Section {
        Text(
            text = "Content inside the section",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltPreviewWrapper::class)
@Composable
fun SectionDarkThemeScreenshot() {
    Section(title = "Shared with") {
        Text(
            text = "Content inside the section",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
