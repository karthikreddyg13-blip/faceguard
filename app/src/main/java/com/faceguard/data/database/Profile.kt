package com.faceguard.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val relation: String,
    val isOwner: Boolean,
    val faceVector: ByteArray?,
    val createdAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Profile

        if (id != other.id) return false
        if (name != other.name) return false
        if (relation != other.relation) return false
        if (isOwner != other.isOwner) return false
        if (faceVector != null) {
            if (other.faceVector == null) return false
            if (!faceVector.contentEquals(other.faceVector)) return false
        } else if (other.faceVector != null) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + relation.hashCode()
        result = 31 * result + isOwner.hashCode()
        result = 31 * result + (faceVector?.contentHashCode() ?: 0)
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
