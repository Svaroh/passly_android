package net.svaroh.passly.feature.autofill.autofill

import android.content.Context
import android.widget.RemoteViews
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import net.svaroh.passly.core.compose.PassboltGlanceColorScheme
import net.svaroh.passly.core.ui.R as CoreUiR

@Composable
internal fun AutofillDropdownGlanceContent(
    @StringRes textResId: Int,
) {
    val context = LocalContext.current
    GlanceTheme(colors = PassboltGlanceColorScheme) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                GlanceModifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(GlanceTheme.colors.background),
        ) {
            Image(
                provider = ImageProvider(CoreUiR.drawable.ic_logo),
                contentDescription = null,
            )
            Text(
                text = context.getString(textResId),
                style =
                    TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = GlanceTheme.colors.onBackground,
                    ),
                modifier = GlanceModifier.padding(start = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
internal suspend fun composeAutofillDropdownRemoteViews(
    context: Context,
    @StringRes textResId: Int,
): RemoteViews =
    GlanceRemoteViews()
        .compose(
            context = context,
            size = DpSize(300.dp, 60.dp),
        ) {
            AutofillDropdownGlanceContent(textResId)
        }.remoteViews
