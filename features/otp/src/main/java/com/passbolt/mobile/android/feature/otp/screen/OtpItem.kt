package net.svaroh.passly.feature.otp.screen

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import net.svaroh.passly.common.extension.toSingleLine
import net.svaroh.passly.core.compose.Inconsolata
import net.svaroh.passly.core.formatter.OtpFormatter
import net.svaroh.passly.core.ui.R
import net.svaroh.passly.core.ui.controller.TotpComposeController
import net.svaroh.passly.domain.resources.resourceicon.ResourceIconProvider
import net.svaroh.passly.feature.otp.screen.ui.ProgressSource
import net.svaroh.passly.ui.OtpItemWrapper
import net.svaroh.passly.ui.isExpired
import org.koin.compose.koinInject

@Composable
internal fun OtpItem(
    otpItem: OtpItemWrapper,
    resourceIconProvider: ResourceIconProvider,
    onItemClick: (OtpItemWrapper) -> Unit,
    onMoreClick: (OtpItemWrapper) -> Unit,
    showMoreMenu: Boolean = true,
    showEyeIcon: Boolean = true,
    progressSource: ProgressSource? = null,
    otpFormatter: OtpFormatter = koinInject(),
    totpController: TotpComposeController = koinInject(),
) {
    val context = LocalContext.current

    val totpColors =
        totpController.calculateTotpColors(
            remainingSeconds = progressSource?.remainingSeconds,
            expirySeconds = progressSource?.expirySeconds,
            isOtpVisible = progressSource != null,
        )
    val totpAnimations =
        totpController.calculateTotpAnimations(
            remainingSeconds = progressSource?.remainingSeconds,
            expirySeconds = progressSource?.expirySeconds,
        )

    var resourceIcon by remember { mutableStateOf<Drawable?>(null) }
    LaunchedEffect(otpItem) {
        resourceIcon = resourceIconProvider.getResourceIcon(context, otpItem.resource)
    }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable { onItemClick(otpItem) }
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(46.dp, 52.dp)) {
            if (resourceIcon != null) {
                Image(
                    painter = rememberDrawablePainter(resourceIcon),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(40.dp)
                            .align(Alignment.CenterStart),
                )
            }

            if (otpItem.resource.isExpired()) {
                Image(
                    painter = painterResource(R.drawable.ic_excl_indicator),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(12.dp)
                            .align(Alignment.BottomEnd),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text =
                    otpItem.resource.metadataJsonModel.name
                        .toSingleLine(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(
                    text =
                        otpFormatter.format(
                            if (otpItem.isVisible) {
                                otpItem.otpValue.orEmpty()
                            } else {
                                stringResource(
                                    net.svaroh.passly.core.localization.R.string.otp_hide_otp,
                                )
                            },
                        ),
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                    fontFamily = Inconsolata,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = totpColors.otpTextColor,
                )

                Spacer(modifier = Modifier.width(16.dp))

                when {
                    otpItem.isRefreshing -> {
                        Icon(
                            painter = painterResource(R.drawable.ic_refresh),
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .size(20.dp)
                                    .rotate(totpAnimations.rotationAngle),
                        )
                    }
                    progressSource != null -> {
                        CircularProgressIndicator(
                            progress = { totpAnimations.animatedProgress },
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 3.dp,
                            color = totpColors.progressIndicatorColor,
                            trackColor = Color.Transparent,
                        )
                    }
                    showEyeIcon -> {
                        Icon(
                            painter = painterResource(R.drawable.ic_eye_visible),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp, 20.dp),
                        )
                    }
                }
            }
        }

        if (showMoreMenu) {
            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { onMoreClick(otpItem) },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = null)
            }
        }
    }
}
