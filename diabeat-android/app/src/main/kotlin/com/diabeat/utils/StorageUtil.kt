package com.diabeat.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

object StorageUtil {
    fun saveBitmap(context: Context, bitmap: Bitmap, filename: String): Uri? {
        return try {
            val imagesDir = File(context.getExternalFilesDir(null), "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            
            val imageFile = File(imagesDir, "$filename.jpg")
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            Uri.fromFile(imageFile)
        } catch (e: Exception) {
            null
        }
    }
}

