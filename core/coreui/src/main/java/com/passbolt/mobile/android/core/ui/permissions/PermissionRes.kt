package net.svaroh.passly.core.ui.permissions

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.svaroh.passly.ui.ResourcePermission
import net.svaroh.passly.ui.ResourcePermission.OWNER
import net.svaroh.passly.ui.ResourcePermission.READ
import net.svaroh.passly.ui.ResourcePermission.UPDATE
import net.svaroh.passly.core.localization.R as LocalizationR
import net.svaroh.passly.core.ui.R as CoreUiR

@DrawableRes
internal fun getPermissionIconRes(permission: ResourcePermission): Int =
    when (permission) {
        READ -> CoreUiR.drawable.ic_permission_read
        UPDATE -> CoreUiR.drawable.ic_permission_edit
        OWNER -> CoreUiR.drawable.ic_permission_owner
    }

@StringRes
internal fun getPermissionNameRes(permission: ResourcePermission): Int =
    when (permission) {
        READ -> LocalizationR.string.resource_permissions_can_read
        UPDATE -> LocalizationR.string.resource_permissions_can_update
        OWNER -> LocalizationR.string.resource_permissions_is_owner
    }
