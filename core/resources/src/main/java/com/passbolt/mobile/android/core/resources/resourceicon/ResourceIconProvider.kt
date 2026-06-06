package net.svaroh.passly.core.resources.resourceicon

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.view.Gravity
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import net.svaroh.passly.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import net.svaroh.passly.supportedresourceTypes.ContentType
import net.svaroh.passly.supportedresourceTypes.ContentType.PasswordAndDescription
import net.svaroh.passly.supportedresourceTypes.ContentType.PasswordDescriptionTotp
import net.svaroh.passly.supportedresourceTypes.ContentType.PasswordString
import net.svaroh.passly.supportedresourceTypes.ContentType.Totp
import net.svaroh.passly.supportedresourceTypes.ContentType.V5CustomFields
import net.svaroh.passly.supportedresourceTypes.ContentType.V5Default
import net.svaroh.passly.supportedresourceTypes.ContentType.V5DefaultWithTotp
import net.svaroh.passly.supportedresourceTypes.ContentType.V5Note
import net.svaroh.passly.supportedresourceTypes.ContentType.V5PasswordString
import net.svaroh.passly.supportedresourceTypes.ContentType.V5TotpStandalone
import net.svaroh.passly.ui.ResourceAppearanceModel.Companion.DEFAULT_BACKGROUND_COLOR_HEX_STRING
import net.svaroh.passly.ui.ResourceAppearanceModel.Companion.ICON_TYPE_KEEPASS
import net.svaroh.passly.ui.ResourceModel
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import net.svaroh.passly.core.ui.R as CoreUiR

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */
class ResourceIconProvider(
    private val resourceTypeIdToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider,
    private val coroutineLaunchContext: CoroutineLaunchContext,
    getSelectedAccountUseCase: GetSelectedAccountUseCase,
) {
    val selectedAccount: String =
        requireNotNull(
            getSelectedAccountUseCase.execute(Unit).selectedAccount,
        ) { "Encountered null selected account" }

    suspend fun getResourceIcon(
        context: Context,
        resource: ResourceModel,
    ): Drawable {
        val resourceIcon = resource.metadataJsonModel.icon
        return if (resourceIcon != null && resourceIcon.type == ICON_TYPE_KEEPASS) {
            try {
                getKeypassIcon(
                    context,
                    requireNotNull(resourceIcon.value),
                    resourceIcon.backgroundColorHexString,
                )
            } catch (e: Exception) {
                Timber.Forest.e(e, "Error getting keepass icon")
                // Fallback to default icon if keepass icon error
                getIconByResourceType(
                    context,
                    resource.resourceTypeId,
                    resourceIcon.backgroundColorHexString,
                )
            }
        } else {
            getIconByResourceType(
                context,
                resource.resourceTypeId,
                resource.metadataJsonModel.icon?.backgroundColorHexString,
            )
        }
    }

    private suspend fun getIconByResourceType(
        context: Context,
        resourceTypeId: String,
        backgroundHexString: String?,
    ): Drawable {
        val resourceTypeIdToSlugMapping = resourceTypeIdToSlugMappingProvider.provideMappingForAccount(selectedAccount)
        val slug = resourceTypeIdToSlugMapping[UUID.fromString(resourceTypeId)]
        val contentType =
            if (slug != null) {
                ContentType.fromSlug(slug)
            } else {
                Timber.e("Encountered null slug; Resource type mapping size: ${resourceTypeIdToSlugMapping.size} ")
                null
            }

        val drawableRes =
            when (contentType) {
                PasswordAndDescription, V5Default -> CoreUiR.drawable.passbolt_password
                PasswordDescriptionTotp, V5DefaultWithTotp -> CoreUiR.drawable.passbolt_totp_password_with_totp
                PasswordString, V5PasswordString -> CoreUiR.drawable.passbolt_password
                Totp, V5TotpStandalone -> CoreUiR.drawable.passbolt_totp
                V5CustomFields -> CoreUiR.drawable.passbolt_key_value
                V5Note -> CoreUiR.drawable.passbolt_note
                null -> null
            }

        return createCircleDrawableWithIcon(
            backgroundColorHex = backgroundHexString,
            vectorResId = drawableRes,
            context = context,
        )
    }

    private suspend fun createCircleDrawableWithIcon(
        backgroundColorHex: String?,
        @DrawableRes vectorResId: Int?,
        context: Context,
        withSelectedBorder: Boolean = false,
        borderWidthDp: Float = 4f,
    ): Drawable =
        withContext(coroutineLaunchContext.io) {
            val backgroundColor = (backgroundColorHex ?: DEFAULT_BACKGROUND_COLOR_HEX_STRING).toColorInt()
            val density = context.resources.displayMetrics.density
            val iconTint = getContrastingTint(backgroundColor)

            val backgroundShape =
                ShapeDrawable(OvalShape()).apply {
                    paint.color = backgroundColor
                }

            val icon =
                vectorResId?.let { vectorResId ->
                    ContextCompat.getDrawable(context, vectorResId)?.apply {
                        setTint(iconTint)
                    }
                }

            if (icon != null) {
                if (!withSelectedBorder) {
                    LayerDrawable(arrayOf(backgroundShape, icon)).apply {
                        setLayerGravity(1, Gravity.CENTER)
                    }
                } else {
                    val borderColor = ContextCompat.getColor(context, CoreUiR.color.primary)

                    val borderShape =
                        ShapeDrawable(OvalShape()).apply {
                            paint.color = borderColor
                        }

                    LayerDrawable(arrayOf(borderShape, backgroundShape, icon)).apply {
                        val borderInset = (borderWidthDp * density).toInt()
                        setLayerInset(1, borderInset, borderInset, borderInset, borderInset)
                    }
                }
            } else {
                ContextCompat.getDrawable(context, CoreUiR.drawable.ic_empty_placeholder)!!
            }
        }

    private suspend fun createCircleDrawableWithIconByName(
        backgroundColorHex: String?,
        vectorDrawableName: String,
        context: Context,
        withSelectedBorder: Boolean = false,
    ): Drawable {
        @SuppressLint("DiscouragedApi") // only way to load drawable by name
        val drawableResId =
            context.resources.getIdentifier(
                vectorDrawableName,
                "drawable",
                context.packageName,
            )

        return createCircleDrawableWithIcon(
            backgroundColorHex,
            drawableResId,
            context,
            withSelectedBorder = withSelectedBorder,
        )
    }

    suspend fun getKeypassIcon(
        context: Context,
        keepassIconValue: Int,
        backgroundHexString: String?,
        withSelectedBorder: Boolean = false,
    ): Drawable =
        createCircleDrawableWithIconByName(
            backgroundColorHex = backgroundHexString,
            vectorDrawableName = "keepass_$keepassIconValue",
            context = context,
            withSelectedBorder = withSelectedBorder,
        )

    private fun getContrastingTint(backgroundColor: Int): Int {
        val r = Color.red(backgroundColor)
        val g = Color.green(backgroundColor)
        val b = Color.blue(backgroundColor)

        @Suppress("MagicNumber") // Using standard luminance calculation
        val luminance = (299 * r + 587 * g + 114 * b) / 1000

        return if (luminance > TINT_THRESHOLD_LUMINANCE) Color.BLACK else Color.WHITE
    }

    private companion object {
        private const val TINT_THRESHOLD_LUMINANCE = 125
    }
}
