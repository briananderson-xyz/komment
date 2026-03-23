package com.kontourai.komment.data

import kotlinx.coroutines.flow.Flow

class SessionRepository(private val dao: SessionDao) {
    fun getAll(): Flow<List<Session>> = dao.getAll()
    suspend fun getById(id: Long): Session? = dao.getById(id)
    suspend fun getActiveSession(): Session? = dao.getActiveSession()
    suspend fun insert(session: Session): Long = dao.insert(session)
    suspend fun update(session: Session) = dao.update(session)
    suspend fun delete(session: Session) = dao.delete(session)
}

class AnnotationRepository(private val dao: AnnotationDao) {
    fun getBySession(sessionId: Long): Flow<List<Annotation>> = dao.getBySession(sessionId)
    suspend fun getBySessionList(sessionId: Long): List<Annotation> = dao.getBySessionList(sessionId)
    suspend fun getById(id: Long): Annotation? = dao.getById(id)
    suspend fun insert(annotation: Annotation): Long = dao.insert(annotation)
    suspend fun update(annotation: Annotation) = dao.update(annotation)
    suspend fun delete(annotation: Annotation) = dao.delete(annotation)
    suspend fun countBySession(sessionId: Long): Int = dao.countBySession(sessionId)
    suspend fun deleteBySession(sessionId: Long) = dao.deleteBySession(sessionId)
}
