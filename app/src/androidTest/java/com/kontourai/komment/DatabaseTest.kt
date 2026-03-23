package com.kontourai.komment

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kontourai.komment.data.Annotation
import com.kontourai.komment.data.AnnotationDao
import com.kontourai.komment.data.AppDatabase
import com.kontourai.komment.data.Session
import com.kontourai.komment.data.SessionDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var sessionDao: SessionDao
    private lateinit var annotationDao: AnnotationDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionDao = db.sessionDao()
        annotationDao = db.annotationDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    // --- Session Tests ---

    @Test
    fun insertAndRetrieveSession() = runTest {
        val id = sessionDao.insert(Session(name = "Test Session"))
        val session = sessionDao.getById(id)
        assertNotNull(session)
        assertEquals("Test Session", session!!.name)
        assertEquals("active", session.status)
    }

    @Test
    fun getActiveSession() = runTest {
        sessionDao.insert(Session(name = "Active One"))
        sessionDao.insert(Session(name = "Closed One", status = "closed"))

        val active = sessionDao.getActiveSession()
        assertNotNull(active)
        assertEquals("Active One", active!!.name)
    }

    @Test
    fun getActiveSessionReturnsNullWhenNoneActive() = runTest {
        sessionDao.insert(Session(name = "Closed", status = "closed"))
        val active = sessionDao.getActiveSession()
        assertNull(active)
    }

    @Test
    fun updateSession() = runTest {
        val id = sessionDao.insert(Session(name = "Original"))
        val session = sessionDao.getById(id)!!
        sessionDao.update(session.copy(name = "Renamed", status = "closed"))

        val updated = sessionDao.getById(id)!!
        assertEquals("Renamed", updated.name)
        assertEquals("closed", updated.status)
    }

    @Test
    fun deleteSession() = runTest {
        val id = sessionDao.insert(Session(name = "To Delete"))
        val session = sessionDao.getById(id)!!
        sessionDao.delete(session)
        assertNull(sessionDao.getById(id))
    }

    @Test
    fun getAllSessionsOrderedByCreatedAtDesc() = runTest {
        sessionDao.insert(Session(name = "First", createdAt = 1000L))
        sessionDao.insert(Session(name = "Second", createdAt = 2000L))
        sessionDao.insert(Session(name = "Third", createdAt = 3000L))

        val sessions = sessionDao.getAll().first()
        assertEquals(3, sessions.size)
        assertEquals("Third", sessions[0].name)
        assertEquals("Second", sessions[1].name)
        assertEquals("First", sessions[2].name)
    }

    // --- Annotation Tests ---

    @Test
    fun insertAndRetrieveAnnotation() = runTest {
        val sessionId = sessionDao.insert(Session(name = "S"))
        val annId = annotationDao.insert(
            Annotation(sessionId = sessionId, selectedText = "hello", comment = "world")
        )
        val ann = annotationDao.getById(annId)
        assertNotNull(ann)
        assertEquals("hello", ann!!.selectedText)
        assertEquals("world", ann.comment)
        assertEquals(sessionId, ann.sessionId)
    }

    @Test
    fun getAnnotationsBySessionOrderedDesc() = runTest {
        val sessionId = sessionDao.insert(Session(name = "S"))
        annotationDao.insert(Annotation(sessionId = sessionId, comment = "first", timestamp = 1000L))
        annotationDao.insert(Annotation(sessionId = sessionId, comment = "second", timestamp = 2000L))
        annotationDao.insert(Annotation(sessionId = sessionId, comment = "third", timestamp = 3000L))

        val annotations = annotationDao.getBySession(sessionId).first()
        assertEquals(3, annotations.size)
        assertEquals("third", annotations[0].comment)
        assertEquals("first", annotations[2].comment)
    }

    @Test
    fun getAnnotationsBySessionListOrderedAsc() = runTest {
        val sessionId = sessionDao.insert(Session(name = "S"))
        annotationDao.insert(Annotation(sessionId = sessionId, comment = "first", timestamp = 1000L))
        annotationDao.insert(Annotation(sessionId = sessionId, comment = "second", timestamp = 2000L))

        val annotations = annotationDao.getBySessionList(sessionId)
        assertEquals(2, annotations.size)
        assertEquals("first", annotations[0].comment)
        assertEquals("second", annotations[1].comment)
    }

    @Test
    fun updateAnnotationComment() = runTest {
        val sessionId = sessionDao.insert(Session(name = "S"))
        val annId = annotationDao.insert(
            Annotation(sessionId = sessionId, comment = "original")
        )
        val ann = annotationDao.getById(annId)!!
        annotationDao.update(ann.copy(comment = "edited"))

        val updated = annotationDao.getById(annId)!!
        assertEquals("edited", updated.comment)
    }

    @Test
    fun deleteAnnotation() = runTest {
        val sessionId = sessionDao.insert(Session(name = "S"))
        val annId = annotationDao.insert(
            Annotation(sessionId = sessionId, comment = "to delete")
        )
        val ann = annotationDao.getById(annId)!!
        annotationDao.delete(ann)
        assertNull(annotationDao.getById(annId))
    }

    @Test
    fun countBySession() = runTest {
        val sessionId = sessionDao.insert(Session(name = "S"))
        annotationDao.insert(Annotation(sessionId = sessionId, comment = "a"))
        annotationDao.insert(Annotation(sessionId = sessionId, comment = "b"))
        annotationDao.insert(Annotation(sessionId = sessionId, comment = "c"))

        assertEquals(3, annotationDao.countBySession(sessionId))
    }

    @Test
    fun deleteBySession() = runTest {
        val sessionId = sessionDao.insert(Session(name = "S"))
        annotationDao.insert(Annotation(sessionId = sessionId, comment = "a"))
        annotationDao.insert(Annotation(sessionId = sessionId, comment = "b"))

        annotationDao.deleteBySession(sessionId)
        assertEquals(0, annotationDao.countBySession(sessionId))
    }

    @Test
    fun cascadeDeleteRemovesAnnotations() = runTest {
        val sessionId = sessionDao.insert(Session(name = "S"))
        annotationDao.insert(Annotation(sessionId = sessionId, comment = "a"))
        annotationDao.insert(Annotation(sessionId = sessionId, comment = "b"))

        val session = sessionDao.getById(sessionId)!!
        sessionDao.delete(session)

        assertEquals(0, annotationDao.countBySession(sessionId))
    }

    @Test
    fun annotationsIsolatedBetweenSessions() = runTest {
        val s1 = sessionDao.insert(Session(name = "S1"))
        val s2 = sessionDao.insert(Session(name = "S2"))
        annotationDao.insert(Annotation(sessionId = s1, comment = "s1-a"))
        annotationDao.insert(Annotation(sessionId = s1, comment = "s1-b"))
        annotationDao.insert(Annotation(sessionId = s2, comment = "s2-a"))

        assertEquals(2, annotationDao.countBySession(s1))
        assertEquals(1, annotationDao.countBySession(s2))
    }

    @Test
    fun annotationWithAllFields() = runTest {
        val sessionId = sessionDao.insert(Session(name = "S"))
        val annId = annotationDao.insert(
            Annotation(
                sessionId = sessionId,
                screenshotPath = "/path/to/img.jpg",
                selectedText = "selected",
                comment = "my comment",
                sourceApp = "com.example.app",
                timestamp = 1700000000000L
            )
        )
        val ann = annotationDao.getById(annId)!!
        assertEquals("/path/to/img.jpg", ann.screenshotPath)
        assertEquals("selected", ann.selectedText)
        assertEquals("my comment", ann.comment)
        assertEquals("com.example.app", ann.sourceApp)
        assertEquals(1700000000000L, ann.timestamp)
    }

    @Test
    fun annotationWithNullOptionalFields() = runTest {
        val sessionId = sessionDao.insert(Session(name = "S"))
        val annId = annotationDao.insert(
            Annotation(sessionId = sessionId, comment = "just a comment")
        )
        val ann = annotationDao.getById(annId)!!
        assertNull(ann.screenshotPath)
        assertNull(ann.selectedText)
        assertNull(ann.sourceApp)
    }
}
