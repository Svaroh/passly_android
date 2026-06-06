package net.svaroh.passly.entity.permission

import net.svaroh.passly.entity.resource.Permission

data class GroupPermission(
    val groupId: String,
    val permission: Permission,
    val permissionId: String,
    val groupName: String,
)
