package com.passbolt.mobile.android.core.ui.circlestepsview

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.core.ui.R
import com.passbolt.mobile.android.core.ui.screenshot.ScreenshotContainer

private val semiBold = SpanStyle(fontWeight = FontWeight.SemiBold)

@PreviewTest
@Preview(showBackground = true)
@Composable
fun CircleStepsViewNumberedScreenshot() {
    ScreenshotContainer {
        CircleStepsView(
            steps =
                listOf(
                    CircleStepItemModel(text = buildAnnotatedString { append("Open the web app") }),
                    CircleStepItemModel(text = buildAnnotatedString { append("Go to your profile") }),
                    CircleStepItemModel(
                        text =
                            buildAnnotatedString {
                                append("Select ")
                                withStyle(semiBold) { append("Mobile transfer") }
                            },
                    ),
                ),
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun CircleStepsViewWithIconsScreenshot() {
    ScreenshotContainer {
        CircleStepsView(
            steps =
                listOf(
                    CircleStepItemModel(
                        text = buildAnnotatedString { append("Open the autofill settings") },
                        icon = CircleStepIcon.Drawable(R.drawable.passbolt_with_bg),
                    ),
                    CircleStepItemModel(
                        text = buildAnnotatedString { append("Pick Passbolt as the provider") },
                        icon =
                            CircleStepIcon.Content {
                                Image(
                                    painter = painterResource(R.drawable.ic_logo),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                    ),
                    CircleStepItemModel(text = buildAnnotatedString { append("Sign in on this device") }),
                ),
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun CircleStepsViewDarkThemeScreenshot() {
    ScreenshotContainer(isDarkTheme = true) {
        CircleStepsView(
            steps =
                listOf(
                    CircleStepItemModel(text = buildAnnotatedString { append("Open the web app") }),
                    CircleStepItemModel(text = buildAnnotatedString { append("Go to your profile") }),
                ),
        )
    }
}

@PreviewTest
@Preview(showBackground = true, fontScale = 1.5f)
@Composable
fun CircleStepsViewLargeFontScreenshot() {
    ScreenshotContainer {
        CircleStepsView(
            steps =
                listOf(
                    CircleStepItemModel(text = buildAnnotatedString { append("Open the web app") }),
                    CircleStepItemModel(text = buildAnnotatedString { append("Go to your profile") }),
                ),
        )
    }
}
