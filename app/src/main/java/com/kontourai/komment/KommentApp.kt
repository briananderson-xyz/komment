package com.kontourai.komment

import android.app.Application
import com.kontourai.komment.data.AnnotationRepository
import com.kontourai.komment.data.AppDatabase
import com.kontourai.komment.data.SessionRepository

class KommentApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val sessionRepository by lazy { SessionRepository(database.sessionDao()) }
    val annotationRepository by lazy { AnnotationRepository(database.annotationDao()) }
}
