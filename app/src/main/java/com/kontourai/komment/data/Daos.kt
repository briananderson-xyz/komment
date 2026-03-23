package com.kontourai.komment.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: Session): Long

    @Update
    suspend fun update(session: Session)

    @Delete
    suspend fun delete(session: Session)

    @Query("SELECT * FROM sessions ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: Long): Session?

    @Query("SELECT * FROM sessions WHERE status = 'active' LIMIT 1")
    suspend fun getActiveSession(): Session?
}

@Dao
interface AnnotationDao {
    @Insert
    suspend fun insert(annotation: Annotation): Long

    @Update
    suspend fun update(annotation: Annotation)

    @Delete
    suspend fun delete(annotation: Annotation)

    @Query("SELECT * FROM annotations WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getBySession(sessionId: Long): Flow<List<Annotation>>

    @Query("SELECT * FROM annotations WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getBySessionList(sessionId: Long): List<Annotation>

    @Query("SELECT * FROM annotations WHERE id = :id")
    suspend fun getById(id: Long): Annotation?

    @Query("SELECT COUNT(*) FROM annotations WHERE sessionId = :sessionId")
    suspend fun countBySession(sessionId: Long): Int

    @Query("DELETE FROM annotations WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: Long)
}
