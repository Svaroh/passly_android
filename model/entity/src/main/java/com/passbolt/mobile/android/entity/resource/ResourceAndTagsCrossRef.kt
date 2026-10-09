package net.svaroh.passly.entity.resource

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    primaryKeys = ["tagId", "resourceId"],
    indices = [Index(value = ["resourceId"])],
    foreignKeys = [
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Resource::class,
            parentColumns = ["resourceId"],
            childColumns = ["resourceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ResourceAndTagsCrossRef(
    val tagId: String,
    val resourceId: String,
)
