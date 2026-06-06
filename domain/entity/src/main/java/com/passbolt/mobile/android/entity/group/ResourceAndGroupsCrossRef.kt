package net.svaroh.passly.entity.group

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import net.svaroh.passly.entity.resource.Permission
import net.svaroh.passly.entity.resource.Resource

@Entity(
    primaryKeys = ["resourceId", "groupId"],
    foreignKeys = [
        ForeignKey(
            entity = Resource::class,
            parentColumns = ["resourceId"],
            childColumns = ["resourceId"],
            onDelete = CASCADE,
        ),
        ForeignKey(
            entity = UsersGroup::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = CASCADE,
        ),
    ],
)
data class ResourceAndGroupsCrossRef(
    val resourceId: String,
    val groupId: String,
    val permission: Permission,
    val permissionId: String,
)
