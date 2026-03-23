package com.molysystems.pinvault.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recovery_codes",
    foreignKeys = [
        ForeignKey(
            entity = AppEntry::class,
            parentColumns = ["id"],
            childColumns = ["appEntryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("appEntryId")]
)
data class RecoveryCode(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appEntryId: Long,
    val serviceLabel: String,
    val encryptedCode: ByteArray,
    val iv: ByteArray,
    val isUsed: Boolean = false,
    val usedAt: Long? = null,
    val sortOrder: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RecoveryCode
        return id == other.id &&
            appEntryId == other.appEntryId &&
            serviceLabel == other.serviceLabel &&
            encryptedCode.contentEquals(other.encryptedCode) &&
            iv.contentEquals(other.iv) &&
            isUsed == other.isUsed &&
            usedAt == other.usedAt &&
            sortOrder == other.sortOrder
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + appEntryId.hashCode()
        result = 31 * result + serviceLabel.hashCode()
        result = 31 * result + encryptedCode.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + isUsed.hashCode()
        result = 31 * result + (usedAt?.hashCode() ?: 0)
        result = 31 * result + sortOrder
        return result
    }
}
