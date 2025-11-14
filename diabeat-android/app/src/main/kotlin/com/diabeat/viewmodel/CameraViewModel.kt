package com.diabeat.viewmodel

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diabeat.utils.ImageUtil
import com.diabeat.utils.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale

class CameraViewModel : ViewModel() {
    var currentImageUri by mutableStateOf<Uri?>(null)
    var imageCapture: ImageCapture? = null
    
    fun captureImage(context: Context, onComplete: (Uri) -> Unit) {
        val capture = imageCapture ?: run {
            Log.e("CameraViewModel", "ImageCapture not initialized")
            return
        }
        
        // 创建时间戳文件名
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault())
            .format(System.currentTimeMillis())
        
        // 创建输出选项
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/DiabEat")
            }
        }
        
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()
        
        // 拍照
        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: return
                    Log.d("CameraViewModel", "Photo saved: $savedUri")
                    currentImageUri = savedUri
                    onComplete(savedUri)
                }
                
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraViewModel", "Photo capture failed: ${exc.message}", exc)
                }
            }
        )
    }
    
    fun importImage(context: Context, uri: Uri, onComplete: (Uri) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = ImageUtil.getBitmapFromUri(context, uri)
            bitmap?.let {
                val savedUri = StorageUtil.saveBitmap(context, it, System.currentTimeMillis().toString())
                savedUri?.let { saved ->
                    currentImageUri = saved
                    onComplete(saved)
                }
            }
        }
    }
}

