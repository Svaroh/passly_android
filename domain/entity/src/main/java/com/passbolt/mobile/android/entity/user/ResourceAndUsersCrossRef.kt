package net.svaroh.passly.entity.user

import androidx.room.Entity
import androidx.room.ForeignKey
import net.svaroh.passly.entity.resource.Permission
import net.svaroh.passly.entity.resource.Resource

@Entity(
    primaryKeys = ["resourceId", "userId"],
    foreignKeys = [
        ForeignKey(
            entity = Resource::class,
            parentColumns = ["resourceId"],
            childColumns = ["resourceId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ResourceAndUsersCrossRef(
    val resourceId: String,
    val userId: String,
    val permission: Permission,
    val permissionId: String,
)
