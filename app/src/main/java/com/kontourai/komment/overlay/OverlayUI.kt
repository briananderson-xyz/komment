package com.kontourai.komment.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kontourai.komment.data.Annotation
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OverlayUI(
    isExpanded: StateFlow<Boolean>,
    isVisible: StateFlow<Boolean>,
    annotations: StateFlow<List<Annotation>>,
    onToggleExpand: () -> Unit,
    onCollapse: () -> Unit,
    onClose: () -> Unit,
    onScreenshot: () -> Unit,
    onAddText: () -> Unit,
    onCopyAll: () -> Unit,
    onViewAll: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onSaveAnnotation: (String?, String) -> Unit,
    onDeleteAnnotation: (Annotation) -> Unit,
    onUpdateComment: (Annotation, String) -> Unit
) {
    val expanded by isExpanded.collectAsState()
    val visible by isVisible.collectAsState()
    val annotationList by annotations.collectAsState()

    var showTextInput by remember { mutableStateOf(false) }
    var showAnnotationList by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var inputComment by remember { mutableStateOf("") }

    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        if (!expanded) {
            // Collapsed bubble
            FloatingActionButton(
                onClick = onToggleExpand,
                modifier = Modifier
                    .size(56.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, offset ->
                                change.consume()
                                onDrag(offset.x, offset.y)
                            },
                            onDragEnd = onDragEnd
                        )
                    },
                shape = CircleShape,
                containerColor = Color(0xFF6750A4),
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Text("K", color = Color.White, fontSize = 20.sp)
            }
        } else if (showTextInput) {
            // Text input panel
            Card(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, offset ->
                                change.consume()
                                onDrag(offset.x, offset.y)
                            },
                            onDragEnd = onDragEnd
                        )
                    },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Add Annotation", color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            OverlayService.instance?.setNeedsFocus(true)
                        },
                        label = { Text("Selected text (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = inputComment,
                        onValueChange = {
                            inputComment = it
                            OverlayService.instance?.setNeedsFocus(true)
                        },
                        label = { Text("Comment") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            showTextInput = false
                            inputText = ""
                            inputComment = ""
                            OverlayService.instance?.setNeedsFocus(false)
                        }) {
                            Text("Cancel", color = Color(0xFFCAC4D0))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = {
                            onSaveAnnotation(
                                inputText.ifBlank { null },
                                inputComment
                            )
                            showTextInput = false
                            inputText = ""
                            inputComment = ""
                            OverlayService.instance?.setNeedsFocus(false)
                        }) {
                            Text("Save", color = Color(0xFFD0BCFF))
                        }
                    }
                }
            }
        } else if (showAnnotationList) {
            // Annotation list panel
            Card(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, offset ->
                                change.consume()
                                onDrag(offset.x, offset.y)
                            },
                            onDragEnd = onDragEnd
                        )
                    },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Annotations (${annotationList.size})",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        IconButton(onClick = { showAnnotationList = false }) {
                            Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    if (annotationList.isEmpty()) {
                        Text(
                            "No annotations yet",
                            color = Color(0xFF938F99),
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.height(300.dp)) {
                            items(annotationList, key = { it.id }) { annotation ->
                                AnnotationListItem(
                                    annotation = annotation,
                                    onDelete = onDeleteAnnotation,
                                    onUpdateComment = onUpdateComment
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Expanded toolbar
            Card(
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, offset ->
                                change.consume()
                                onDrag(offset.x, offset.y)
                            },
                            onDragEnd = onDragEnd
                        )
                    },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F))
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(4.dp)
                            .background(Color(0xFF49454F), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onScreenshot, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.CameraAlt, "Screenshot", tint = Color(0xFFD0BCFF), modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = {
                            showTextInput = true
                            onAddText()
                        }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Add, "Add Text", tint = Color(0xFFD0BCFF), modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { showAnnotationList = true }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.List, "View All", tint = Color(0xFFD0BCFF), modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onCopyAll, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.ContentCopy, "Copy All", tint = Color(0xFFD0BCFF), modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onCollapse, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.KeyboardArrowDown, "Collapse", tint = Color(0xFF938F99), modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, "Close", tint = Color(0xFF938F99), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnotationListItem(
    annotation: Annotation,
    onDelete: (Annotation) -> Unit,
    onUpdateComment: (Annotation, String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    var isEditing by remember { mutableStateOf(false) }
    var editComment by remember { mutableStateOf(annotation.comment) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    dateFormat.format(Date(annotation.timestamp)),
                    color = Color(0xFF938F99),
                    fontSize = 11.sp
                )
                Row {
                    IconButton(
                        onClick = {
                            isEditing = !isEditing
                            editComment = annotation.comment
                            OverlayService.instance?.setNeedsFocus(isEditing)
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Edit, "Edit", tint = Color(0xFF938F99), modifier = Modifier.size(14.dp))
                    }
                    IconButton(
                        onClick = { onDelete(annotation) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Delete, "Delete", tint = Color(0xFF938F99), modifier = Modifier.size(14.dp))
                    }
                }
            }

            annotation.screenshotPath?.let {
                Text("[Screenshot]", color = Color(0xFFD0BCFF), fontSize = 11.sp)
            }

            annotation.selectedText?.let { text ->
                Text(
                    text,
                    color = Color(0xFFCAC4D0),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .background(Color(0xFF1C1B1F), RoundedCornerShape(4.dp))
                        .padding(4.dp)
                )
            }

            if (isEditing) {
                OutlinedTextField(
                    value = editComment,
                    onValueChange = { editComment = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        isEditing = false
                        OverlayService.instance?.setNeedsFocus(false)
                    }) {
                        Text("Cancel", color = Color(0xFFCAC4D0), fontSize = 11.sp)
                    }
                    TextButton(onClick = {
                        onUpdateComment(annotation, editComment)
                        isEditing = false
                        OverlayService.instance?.setNeedsFocus(false)
                    }) {
                        Text("Save", color = Color(0xFFD0BCFF), fontSize = 11.sp)
                    }
                }
            } else if (annotation.comment.isNotBlank()) {
                Text(
                    annotation.comment,
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
