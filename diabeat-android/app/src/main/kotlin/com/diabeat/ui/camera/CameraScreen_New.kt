package com.diabeat.ui.camera

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.diabeat.R
import com.diabeat.viewmodel.CameraViewModel

/**
 * 相机屏幕 - 照搬 rock-android 设计
 * 包含：相机预览 + 底部3按钮（相册、拍照、提示）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen_New(
    viewModel: CameraViewModel,
    onBack: () -> Unit,
    onComplete: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // 图片选择器
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            viewModel.importImage(context, selectedUri) { savedUri ->
                onComplete(savedUri)
            }
        }
    }
    
    // 相机预览
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    
    // 绑定相机生命周期
    LaunchedEffect(previewView) {
        previewView?.let { view ->
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(view.surfaceProvider)
                }
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 相机预览区域
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            // CameraX 预览
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        previewView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // 顶部返回按钮
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            // 中间提示框（可选）
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 扫描框图标（可以添加一个矩形框）
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .background(Color.Transparent)
                )
            }
        }
        
        // 底部控制栏 - 照搬 rock-android 的 CameraBottomActionView
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            CameraBottomControls(
                onGalleryClick = {
                    imagePicker.launch("image/*")
                },
                onTakePhoto = {
                    viewModel.captureImage(context) { uri ->
                        onComplete(uri)
                    }
                },
                onTipsClick = {
                    // TODO: 显示提示
                }
            )
        }
    }
}

/**
 * 底部控制栏 - 完全照搬 rock-android 的设计
 */
@Composable
private fun CameraBottomControls(
    onGalleryClick: () -> Unit,
    onTakePhoto: () -> Unit,
    onTipsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：相册按钮
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .clickable { onGalleryClick() }
        ) {
            Icon(
                imageVector = Icons.Default.Photo,
                contentDescription = stringResource(R.string.import_from_gallery),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.import_from_gallery),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                maxLines = 2
            )
        }
        
        // 中间：拍照按钮（大圆形）
        Surface(
            modifier = Modifier
                .size(80.dp)
                .clickable { onTakePhoto() },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // 内圆
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = Color.White
                ) {}
            }
        }
        
        // 右侧：提示按钮
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .clickable { onTipsClick() }
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.tips),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp
            )
        }
    }
}
