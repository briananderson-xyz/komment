package com.kontourai.komment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.kontourai.komment.overlay.OverlayService

class TextReceiverActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
        finish()
    }

    private fun handleIntent(intent: Intent) {
        val text = when (intent.action) {
            Intent.ACTION_PROCESS_TEXT -> {
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            }
            Intent.ACTION_SEND -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)
            }
            else -> null
        }

        if (text.isNullOrBlank()) {
            Toast.makeText(this, "No text received", Toast.LENGTH_SHORT).show()
            return
        }

        val service = OverlayService.instance
        if (service != null) {
            service.addTextAnnotation(text)
            Toast.makeText(this, "Text added to review", Toast.LENGTH_SHORT).show()
        } else {
            // Start the overlay service first, then add the text
            val serviceIntent = Intent(this, OverlayService::class.java).apply {
                action = OverlayService.ACTION_ADD_TEXT
                putExtra(OverlayService.EXTRA_TEXT, text)
            }
            startForegroundService(serviceIntent)
            Toast.makeText(this, "Started review with text", Toast.LENGTH_SHORT).show()
        }
    }
}
