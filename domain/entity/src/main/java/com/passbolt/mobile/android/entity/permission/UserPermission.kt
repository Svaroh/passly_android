package net.svaroh.passly.entity.permission

import net.svaroh.passly.entity.resource.Permission

data class UserPermission(
    val userId: String,
    val permission: Permission,
    val permissionId: String,
    val firstName: String?,
    val lastName: String?,
    val avatarUrl: String?,
    val userName: String,
    val fingerprint: String,
    val disabled: Boolean,
)
