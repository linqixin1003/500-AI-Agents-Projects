package com.diabeat.ui.food

import android.Manifest
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(
    onBarcodeScanned: (String) -> Unit,
    onBack: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var flashEnabled by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Barcode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { flashEnabled = !flashEnabled }) {
                        Icon(
                            if (flashEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            if (flashEnabled) "关闭闪光灯" else "打开闪光灯"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                cameraPermissionState.status.isGranted -> {
                    CameraPreviewWithScanner(
                        flashEnabled = flashEnabled,
                        onBarcodeDetected = { barcode ->
                            Log.d("BarcodeScanner", "扫描到条形码: $barcode")
                            onBarcodeScanned(barcode)
                        }
                    )
                    
                    // 扫描框覆盖层
                    ScannerOverlay()
                    
                    // 提示文字
                    Text(
                        "Align barcode within frame",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 100.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                else -> {
                    // 权限未授予
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Camera permission required")
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreviewWithScanner(
    flashEnabled: Boolean,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    var lastScannedBarcode by remember { mutableStateOf<String?>(null) }
    var lastScanTime by remember { mutableStateOf(0L) }
    
    DisposableEffect(flashEnabled) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            // Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            
            // Image Analysis for ML Kit
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                        scanBarcode(imageProxy) { barcode ->
                            val currentTime = System.currentTimeMillis()
                            // 防抖：相同条码2秒内只扫描一次
                            if (barcode != lastScannedBarcode || currentTime - lastScanTime > 2000) {
                                lastScannedBarcode = barcode
                                lastScanTime = currentTime
                                onBarcodeDetected(barcode)
                            }
                        }
                    }
                }
            
            // Camera selector (后置摄像头)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                
                // 控制闪光灯
                camera.cameraControl.enableTorch(flashEnabled)
                
            } catch (e: Exception) {
                Log.e("CameraPreview", "绑定失败", e)
            }
        }, ContextCompat.getMainExecutor(context))
        
        onDispose {
            cameraProviderFuture.get().unbindAll()
        }
    }
    
    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * 使用ML Kit扫描条形码
 */
private fun scanBarcode(imageProxy: ImageProxy, onBarcodeDetected: (String) -> Unit) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val scanner = BarcodeScanning.getClient()
        
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    when (barcode.valueType) {
                        Barcode.TYPE_PRODUCT -> {
                            barcode.rawValue?.let { onBarcodeDetected(it) }
                        }
                        else -> {
                            // 其他类型的条形码也支持
                            barcode.rawValue?.let { onBarcodeDetected(it) }
                        }
                    }
                }
            }
            .addOnFailureListener {
                Log.e("MLKit", "条码扫描失败", it)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

/**
 * 扫描框覆盖层
 */
@Composable
fun ScannerOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        // 扫描框尺寸
        val scanWidth = canvasWidth * 0.8f
        val scanHeight = 200f
        
        // 扫描框位置（居中）
        val left = (canvasWidth - scanWidth) / 2
        val top = (canvasHeight - scanHeight) / 2
        
        // 绘制半透明黑色背景（扫描框外的区域）
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            size = Size(canvasWidth, top)  // 上方
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(0f, top + scanHeight),
            size = Size(canvasWidth, canvasHeight - top - scanHeight)  // 下方
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(0f, top),
            size = Size(left, scanHeight)  // 左侧
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(left + scanWidth, top),
            size = Size(canvasWidth - left - scanWidth, scanHeight)  // 右侧
        )
        
        // 绘制扫描框边框（白色虚线）
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(scanWidth, scanHeight),
            cornerRadius = CornerRadius(16f, 16f),
            style = Stroke(
                width = 4f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
            )
        )
        
        // 绘制四个角（强调扫描框）
        val cornerLength = 40f
        val cornerWidth = 6f
        
        // 左上角
        drawLine(Color.Green, Offset(left, top), Offset(left + cornerLength, top), cornerWidth)
        drawLine(Color.Green, Offset(left, top), Offset(left, top + cornerLength), cornerWidth)
        
        // 右上角
        drawLine(Color.Green, Offset(left + scanWidth, top), Offset(left + scanWidth - cornerLength, top), cornerWidth)
        drawLine(Color.Green, Offset(left + scanWidth, top), Offset(left + scanWidth, top + cornerLength), cornerWidth)
        
        // 左下角
        drawLine(Color.Green, Offset(left, top + scanHeight), Offset(left + cornerLength, top + scanHeight), cornerWidth)
        drawLine(Color.Green, Offset(left, top + scanHeight), Offset(left, top + scanHeight - cornerLength), cornerWidth)
        
        // 右下角
        drawLine(Color.Green, Offset(left + scanWidth, top + scanHeight), Offset(left + scanWidth - cornerLength, top + scanHeight), cornerWidth)
        drawLine(Color.Green, Offset(left + scanWidth, top + scanHeight), Offset(left + scanWidth, top + scanHeight - cornerLength), cornerWidth)
    }
}
