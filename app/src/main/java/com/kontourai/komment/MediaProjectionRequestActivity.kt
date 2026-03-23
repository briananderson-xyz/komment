package com.kontourai.komment

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.kontourai.komment.overlay.OverlayService
import java.io.File
import java.io.FileOutputStream

class MediaProjectionRequestActivity : androidx.activity.ComponentActivity() {

    companion object {
        private var cachedResultCode: Int? = null
        private var cachedData: Intent? = null
        private var mediaProjection: MediaProjection? = null

        fun clearCache() {
            mediaProjection?.stop()
            mediaProjection = null
            cachedResultCode = null
            cachedData = null
        }
    }

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            cachedResultCode = result.resultCode
            cachedData = result.data
            captureScreen(result.resultCode, result.data!!)
        } else {
            Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
            OverlayService.instance?.setOverlayVisible(true)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (cachedResultCode != null && cachedData != null) {
            captureScreen(cachedResultCode!!, cachedData!!)
        } else {
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
        }
    }

    private fun captureScreen(resultCode: Int, data: Intent) {
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = projectionManager.getMediaProjection(resultCode, data)
        mediaProjection = projection

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(metrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        val virtualDisplay: VirtualDisplay = projection.createVirtualDisplay(
            "KommentCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, null
        )

        // Delay to allow the screen to render without our overlay
        Handler(Looper.getMainLooper()).postDelayed({
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

                // Crop to actual screen size
                val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                if (croppedBitmap !== bitmap) bitmap.recycle()

                val file = saveScreenshot(croppedBitmap)
                croppedBitmap.recycle()

                virtualDisplay.release()

                if (file != null) {
                    OverlayService.instance?.saveAnnotation(screenshotPath = file.absolutePath)
                    Toast.makeText(this, "Screenshot saved", Toast.LENGTH_SHORT).show()
                }
            } else {
                virtualDisplay.release()
                Toast.makeText(this, "Failed to capture screenshot", Toast.LENGTH_SHORT).show()
            }

            OverlayService.instance?.setOverlayVisible(true)
            finish()
        }, 200)
    }

    private fun saveScreenshot(bitmap: Bitmap): File? {
        return try {
            val dir = File(filesDir, "screenshots")
            dir.mkdirs()
            val file = File(dir, "screenshot_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
