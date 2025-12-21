package com.example.myapplication.ui.sheets

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage

import com.example.myapplication.model.Priority
import com.example.myapplication.ui.components.NeumorphicButton
import com.example.myapplication.ui.components.NeumorphicTextField
import com.example.myapplication.ui.theme.NeumorphicColors
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AddTaskSheet(
    onAddTask: (String, String?, LocalDate?, Priority, Int?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.NORMAL) }
    var dueDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var imageUri by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> imageUri = uri?.toString() }
    )

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success -> if (success) imageUri = pendingCameraUri?.toString() }
    )

    fun launchCameraCapture() {
        val uri = createTempImageUri(context.applicationContext)
        pendingCameraUri = uri
        takePictureLauncher.launch(uri)
    }

    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                launchCameraCapture()
            } else {
                showCameraPermissionRationale(context)
            }
        }
    )

    Column(Modifier.padding(24.dp)) {
        Text("Thêm việc mới", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NeumorphicColors.textPrimary)
        Spacer(Modifier.height(24.dp))
        NeumorphicTextField(value = title, onValueChange = { title = it }, placeholder = "Bạn muốn làm gì?")
        Spacer(Modifier.height(16.dp))
        NeumorphicTextField(value = description, onValueChange = { description = it }, placeholder = "Mô tả công việc (tuỳ chọn)")

        Spacer(Modifier.height(16.dp))
        TaskDatePickerCard(dueDate, context) { dueDate = it }

        Spacer(Modifier.height(24.dp))
        TaskPrioritySelector(priority) { priority = it }

        Spacer(Modifier.height(16.dp))
        TaskImagePickerButtons(
            onPickImage = {
                pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onCaptureImage = {
                handleCameraClick(context, { launchCameraCapture() }, requestCameraPermissionLauncher)
            }
        )

        TaskImagePreview(imageUri)

        Spacer(Modifier.height(32.dp))
        NeumorphicButton("Lưu công việc") {
            if (title.isNotBlank()) {
                onAddTask(title, description.takeIf { it.isNotBlank() }, dueDate, priority, null, imageUri)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun createTempImageUri(appContext: Context): Uri {
    val imagesDir = File(appContext.cacheDir, "images").apply { mkdirs() }
    val imageFile = File(imagesDir, "task_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", imageFile)
}

private fun showCameraPermissionRationale(context: Context) {
    val activity = context as? Activity
    val shouldShowRationale = activity?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
    } ?: false
    val message = if (shouldShowRationale) {
        "Cần quyền Camera để chụp ảnh. Vui lòng cấp quyền để tiếp tục."
    } else {
        "Bạn đã từ chối quyền Camera. Hãy bật quyền trong Cài đặt nếu muốn chụp ảnh."
    }
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

@Composable
fun TaskDatePickerCard(dueDate: LocalDate?, context: Context, onDateSelected: (LocalDate) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val now = dueDate ?: LocalDate.now()
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth -> onDateSelected(LocalDate.of(year, month + 1, dayOfMonth)) },
                    now.year, now.monthValue - 1, now.dayOfMonth
                ).show()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NeumorphicColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(NeumorphicColors.darkShadow.copy(0.05f))
                .padding(16.dp)
        ) {
            val text = dueDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "Chọn ngày đến hạn"
            Text(text, color = NeumorphicColors.textPrimary, fontSize = 16.sp)
        }
    }
}

@Composable
fun TaskPrioritySelector(selectedPriority: Priority, onSelect: (Priority) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        Priority.entries.forEach { p ->
            val isSelected = selectedPriority == p
            Card(
                modifier = Modifier.clickable { onSelect(p) }.padding(horizontal = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isSelected) NeumorphicColors.darkShadow.copy(0.15f) else NeumorphicColors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 6.dp)
            ) {
                Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(p.name, color = if (isSelected) NeumorphicColors.textPrimary else NeumorphicColors.textSecondary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
fun TaskImagePickerButtons(onPickImage: () -> Unit, onCaptureImage: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier.weight(1f).clickable { onPickImage() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = NeumorphicColors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Image, contentDescription = "Chọn ảnh", tint = NeumorphicColors.textPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Chọn ảnh", color = NeumorphicColors.textPrimary)
            }
        }

        Card(
            modifier = Modifier.weight(1f).clickable { onCaptureImage() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = NeumorphicColors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Chụp ảnh", tint = NeumorphicColors.textPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Chụp ảnh", color = NeumorphicColors.textPrimary)
            }
        }
    }
}

@Composable
fun TaskImagePreview(imageUri: String?) {
    imageUri?.let { uriString ->
        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NeumorphicColors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            AsyncImage(
                model = Uri.parse(uriString),
                contentDescription = "Ảnh công việc",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        }
    }
}

@Composable
fun AddCollectionSheet(onAddCollection: (String, Color) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    Column(Modifier.padding(24.dp)) {
        Text("Danh mục mới", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NeumorphicColors.textPrimary)
        Spacer(Modifier.height(24.dp))
        NeumorphicTextField(value = name, onValueChange = { name = it }, placeholder = "Tên danh mục")
        Spacer(Modifier.height(32.dp))
        NeumorphicButton("Tạo danh mục") { if (name.isNotBlank()) onAddCollection(name, NeumorphicColors.accentBlue) }
        Spacer(Modifier.height(24.dp))
    }
}