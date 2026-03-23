package com.molysystems.pinvault.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "credential_fields",
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
data class CredentialField(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appEntryId: Long,
    val label: String,
    val encryptedValue: ByteArray,
    val iv: ByteArray,
    val sortOrder: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CredentialField
        return id == other.id &&
            appEntryId == other.appEntryId &&
            label == other.label &&
            encryptedValue.contentEquals(other.encryptedValue) &&
            iv.contentEquals(other.iv) &&
            sortOrder == other.sortOrder
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + appEntryId.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + encryptedValue.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + sortOrder
        return result
    }
}
