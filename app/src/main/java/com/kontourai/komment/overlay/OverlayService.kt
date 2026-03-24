package com.kontourai.komment.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.pm.ServiceInfo
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.kontourai.komment.KommentApp
import com.kontourai.komment.MainActivity
import com.kontourai.komment.data.Annotation
import com.kontourai.komment.data.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId = _currentSessionId.asStateFlow()

    private val _isExpanded = MutableStateFlow(false)
    private val _isVisible = MutableStateFlow(true)
    private val _annotations = MutableStateFlow<List<Annotation>>(emptyList())

    companion object {
        const val CHANNEL_ID = "overlay_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.kontourai.komment.STOP"
        const val ACTION_ADD_TEXT = "com.kontourai.komment.ADD_TEXT"
        const val EXTRA_TEXT = "extra_text"

        var instance: OverlayService? = null
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(
            NOTIFICATION_ID,
            createNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        initSession()
        showOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_ADD_TEXT -> {
                val text = intent.getStringExtra(EXTRA_TEXT)
                if (!text.isNullOrBlank()) {
                    addTextAnnotation(text)
                }
            }
        }
        return START_STICKY
    }

    private fun initSession() {
        val app = application as KommentApp
        serviceScope.launch {
            val existing = app.sessionRepository.getActiveSession()
            val sessionId = if (existing != null) {
                existing.id
            } else {
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                app.sessionRepository.insert(Session(name = "Review - $dateStr"))
            }
            _currentSessionId.value = sessionId
            // Observe annotations
            app.annotationRepository.getBySession(sessionId).collect {
                _annotations.value = it
            }
        }
    }

    private fun showOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setContent {
                val isExpanded = _isExpanded.asStateFlow()
                val isVisible = _isVisible.asStateFlow()
                val annotations = _annotations.asStateFlow()

                OverlayUI(
                    isExpanded = isExpanded,
                    isVisible = isVisible,
                    annotations = annotations,
                    onToggleExpand = { _isExpanded.value = !_isExpanded.value },
                    onCollapse = { _isExpanded.value = false },
                    onClose = { stopSelf() },
                    onScreenshot = { requestScreenshot() },
                    onAddText = { showTextInput() },
                    onCopyAll = { copyAllAnnotations() },
                    onViewAll = { /* handled in OverlayUI */ },
                    onDrag = { dx, dy ->
                        params.x += dx.toInt()
                        params.y += dy.toInt()
                        windowManager.updateViewLayout(overlayView, params)
                    },
                    onDragEnd = {
                        // Snap to nearest edge
                        val display = windowManager.defaultDisplay
                        val screenWidth = display.width
                        params.x = if (params.x < screenWidth / 2) 0 else screenWidth
                        windowManager.updateViewLayout(overlayView, params)
                    },
                    onSaveAnnotation = { text, comment ->
                        saveAnnotation(selectedText = text, comment = comment)
                    },
                    onDeleteAnnotation = { annotation -> deleteAnnotation(annotation) },
                    onUpdateComment = { annotation, newComment -> updateComment(annotation, newComment) }
                )
            }
        }

        overlayView = composeView
        windowManager.addView(composeView, params)
    }

    fun setOverlayVisible(visible: Boolean) {
        _isVisible.value = visible
    }

    fun setNeedsFocus(needsFocus: Boolean) {
        val view = overlayView ?: return
        val params = view.layoutParams as WindowManager.LayoutParams
        if (needsFocus) {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        windowManager.updateViewLayout(view, params)
    }

    private fun requestScreenshot() {
        _isVisible.value = false
        val intent = Intent(this, com.kontourai.komment.MediaProjectionRequestActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    fun performScreenCapture(resultCode: Int, data: Intent) {
        // Must upgrade to MEDIA_PROJECTION type BEFORE calling getMediaProjection on Android 14+
        startForeground(
            NOTIFICATION_ID,
            createNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )

        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = projectionManager.getMediaProjection(resultCode, data)

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(metrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        val virtualDisplay = projection.createVirtualDisplay(
            "KommentCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, null
        )

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val image = imageReader.acquireLatestImage()
                if (image != null) {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * width

                    val bitmap = Bitmap.createBitmap(
                        width + rowPadding / pixelStride, height,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)
                    image.close()

                    val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                    if (croppedBitmap !== bitmap) bitmap.recycle()

                    val file = saveScreenshotFile(croppedBitmap)
                    croppedBitmap.recycle()

                    if (file != null) {
                        saveAnnotation(screenshotPath = file.absolutePath)
                        Toast.makeText(this, "Screenshot saved", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Failed to capture screenshot", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Screenshot failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                virtualDisplay.release()
                projection.stop()
                imageReader.close()
                // Downgrade back to specialUse only
                startForeground(
                    NOTIFICATION_ID,
                    createNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
                setOverlayVisible(true)
            }
        }, 300)
    }

    private fun saveScreenshotFile(bitmap: Bitmap): java.io.File? {
        return try {
            val dir = java.io.File(applicationContext.filesDir, "screenshots")
            dir.mkdirs()
            val file = java.io.File(dir, "screenshot_${System.currentTimeMillis()}.jpg")
            java.io.FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun showTextInput() {
        _isExpanded.value = true
    }

    fun addTextAnnotation(text: String) {
        val sessionId = _currentSessionId.value ?: return
        val app = application as KommentApp
        serviceScope.launch {
            app.annotationRepository.insert(
                Annotation(
                    sessionId = sessionId,
                    selectedText = text,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun saveAnnotation(
        screenshotPath: String? = null,
        selectedText: String? = null,
        comment: String = "",
        sourceApp: String? = null
    ) {
        val sessionId = _currentSessionId.value ?: return
        val app = application as KommentApp
        serviceScope.launch {
            app.annotationRepository.insert(
                Annotation(
                    sessionId = sessionId,
                    screenshotPath = screenshotPath,
                    selectedText = selectedText,
                    comment = comment,
                    sourceApp = sourceApp,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    private fun deleteAnnotation(annotation: Annotation) {
        val app = application as KommentApp
        serviceScope.launch {
            // Clean up screenshot file if exists
            annotation.screenshotPath?.let { path ->
                java.io.File(path).delete()
            }
            app.annotationRepository.delete(annotation)
        }
    }

    private fun updateComment(annotation: Annotation, newComment: String) {
        val app = application as KommentApp
        serviceScope.launch {
            app.annotationRepository.update(annotation.copy(comment = newComment))
        }
    }

    private fun copyAllAnnotations() {
        val app = application as KommentApp
        serviceScope.launch {
            val sessionId = _currentSessionId.value ?: return@launch
            val annotations = app.annotationRepository.getBySessionList(sessionId)
            if (annotations.isEmpty()) {
                android.widget.Toast.makeText(this@OverlayService, "No annotations to copy", android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }
            val compiled = com.kontourai.komment.export.AnnotationCompiler.compileToMarkdown(annotations)
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Komment Review", compiled))
            android.widget.Toast.makeText(this@OverlayService, "Copied ${annotations.size} annotations", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(com.kontourai.komment.R.string.overlay_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, OverlayService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(com.kontourai.komment.R.string.overlay_notification_title))
            .setContentText(getString(com.kontourai.komment.R.string.overlay_notification_text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(openIntent)
            .addAction(
                Notification.Action.Builder(
                    null,
                    getString(com.kontourai.komment.R.string.action_stop),
                    stopIntent
                ).build()
            )
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        instance = null
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        serviceScope.cancel()
        super.onDestroy()
    }
}
