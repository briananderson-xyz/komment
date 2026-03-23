package com.kontourai.komment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kontourai.komment.data.Annotation
import com.kontourai.komment.data.Session
import com.kontourai.komment.overlay.OverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as KommentApp

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val sessions by app.sessionRepository.getAll().collectAsState(initial = emptyList())

                MainScreen(
                    sessions = sessions,
                    onStartReview = { startOverlay() },
                    onCloseSession = { session ->
                        CoroutineScope(Dispatchers.IO).launch {
                            app.sessionRepository.update(session.copy(status = "closed"))
                        }
                    },
                    onDeleteSession = { session ->
                        CoroutineScope(Dispatchers.IO).launch {
                            // Clean up screenshot files
                            val annotations = app.annotationRepository.getBySessionList(session.id)
                            annotations.forEach { ann ->
                                ann.screenshotPath?.let { File(it).delete() }
                            }
                            app.sessionRepository.delete(session)
                        }
                    },
                    onRenameSession = { session, newName ->
                        CoroutineScope(Dispatchers.IO).launch {
                            app.sessionRepository.update(session.copy(name = newName))
                        }
                    },
                    onCopySession = { session ->
                        CoroutineScope(Dispatchers.IO).launch {
                            val annotations = app.annotationRepository.getBySessionList(session.id)
                            val compiled = com.kontourai.komment.export.AnnotationCompiler.compileToMarkdown(annotations)
                            runOnUiThread {
                                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Komment Review", compiled))
                                Toast.makeText(this@MainActivity, "Copied ${annotations.size} annotations", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onShareSession = { session ->
                        CoroutineScope(Dispatchers.IO).launch {
                            val annotations = app.annotationRepository.getBySessionList(session.id)
                            if (annotations.isEmpty()) {
                                runOnUiThread {
                                    Toast.makeText(this@MainActivity, "No annotations to share", Toast.LENGTH_SHORT).show()
                                }
                                return@launch
                            }
                            val compiled = com.kontourai.komment.export.AnnotationCompiler.compileToMarkdown(annotations)
                            val screenshotFiles = annotations.mapNotNull { it.screenshotPath?.let { p -> File(p) } }.filter { it.exists() }

                            runOnUiThread {
                                if (screenshotFiles.isEmpty()) {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, compiled)
                                    }
                                    startActivity(Intent.createChooser(intent, "Share Review"))
                                } else {
                                    val uris = ArrayList(screenshotFiles.map { file ->
                                        androidx.core.content.FileProvider.getUriForFile(
                                            this@MainActivity,
                                            "$packageName.provider",
                                            file
                                        )
                                    })
                                    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                        type = "*/*"
                                        putExtra(Intent.EXTRA_TEXT, compiled)
                                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    startActivity(Intent.createChooser(intent, "Share Review"))
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    private fun startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission required to float above other apps", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }
        val intent = Intent(this, OverlayService::class.java)
        startForegroundService(intent)
        Toast.makeText(this, "Komment overlay started", Toast.LENGTH_SHORT).show()
    }

}

@Composable
fun MainScreen(
    sessions: List<Session>,
    onStartReview: () -> Unit,
    onCloseSession: (Session) -> Unit,
    onDeleteSession: (Session) -> Unit,
    onRenameSession: (Session, String) -> Unit,
    onCopySession: (Session) -> Unit,
    onShareSession: (Session) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text("Komment", color = Color.White, fontSize = 28.sp)
        Text("Review & annotate anything", color = Color(0xFF938F99), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onStartReview,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
        ) {
            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.padding(end = 8.dp))
            Text("Start Review")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Sessions", color = Color(0xFFCAC4D0), fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No review sessions yet.\nTap 'Start Review' to begin.",
                    color = Color(0xFF938F99),
                    fontStyle = FontStyle.Italic
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sessions, key = { it.id }) { session ->
                    SessionCard(
                        session = session,
                        onClose = { onCloseSession(session) },
                        onDelete = { onDeleteSession(session) },
                        onRename = { newName -> onRenameSession(session, newName) },
                        onCopy = { onCopySession(session) },
                        onShare = { onShareSession(session) }
                    )
                }
            }
        }
    }
}

@Composable
fun SessionCard(
    session: Session,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()) }
    var showRename by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(session.name) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(session.name, color = Color.White, fontSize = 15.sp)
                    Text(
                        dateFormat.format(Date(session.createdAt)),
                        color = Color(0xFF938F99),
                        fontSize = 12.sp
                    )
                }
                Text(
                    if (session.status == "active") "Active" else "Closed",
                    color = if (session.status == "active") Color(0xFF4CAF50) else Color(0xFF938F99),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                IconButton(onClick = { showRename = true }) {
                    Icon(Icons.Default.Edit, "Rename", tint = Color(0xFFCAC4D0), modifier = androidx.compose.ui.Modifier.height(18.dp).width(18.dp))
                }
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, "Copy", tint = Color(0xFFCAC4D0), modifier = androidx.compose.ui.Modifier.height(18.dp).width(18.dp))
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, "Share", tint = Color(0xFFCAC4D0), modifier = androidx.compose.ui.Modifier.height(18.dp).width(18.dp))
                }
                if (session.status == "active") {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, "Close Session", tint = Color(0xFFCAC4D0), modifier = androidx.compose.ui.Modifier.height(18.dp).width(18.dp))
                    }
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFEF5350), modifier = androidx.compose.ui.Modifier.height(18.dp).width(18.dp))
                }
            }
        }
    }

    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Rename Session") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Session name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(renameText)
                    showRename = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Session?") },
            text = { Text("This will permanently delete this session and all its annotations.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) { Text("Delete", color = Color(0xFFEF5350)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
