package de.odpisz.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "letters")
data class LetterEntity(
    @PrimaryKey val id: String,
    val createdAtEpochMillis: Long,
    val title: String,
    val originalText: String,
    val maskedText: String,
    val sourceLabel: String?,
    val status: String,
    val analysisJson: String?,
    val replyJson: String?,
    val piiJson: String,
    val deadlineFlags: String,
)

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = PROFILE_ID,
    val fullName: String,
    val street: String,
    val postcode: String,
    val city: String,
    val email: String,
    val phone: String,
) {
    companion object {
        const val PROFILE_ID = 1
    }
}
