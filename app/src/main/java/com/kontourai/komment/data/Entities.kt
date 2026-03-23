package com.kontourai.komment.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "active" // "active" or "closed"
)

@Entity(
    tableName = "annotations",
    foreignKeys = [
        ForeignKey(
            entity = Session::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class Annotation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val screenshotPath: String? = null,
    val selectedText: String? = null,
    val comment: String = "",
    val sourceApp: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
