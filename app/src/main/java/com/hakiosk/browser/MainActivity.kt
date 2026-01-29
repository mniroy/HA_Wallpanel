package com.hakiosk.browser

import android.Manifest
import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.*
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

class MainActivity : AppCompatActivity(), SensorEventListener {
    
    companion object {
        private const val TAG = "HAKiosk"
        private const val PREFS_NAME = "HAKioskPrefs"
        private const val KEY_HOME_URL = "home_url"
        private const val KEY_SCREEN_CONTROL_ENABLED = "screen_control_enabled"
        private const val KEY_MOTION_DETECTION_ENABLED = "motion_detection_enabled"
        private const val KEY_PROXIMITY_ENABLED = "proximity_enabled"
        private const val KEY_PROXIMITY_MODE = "proximity_mode"
        private const val KEY_SCREEN_ROTATION = "screen_rotation"
        private const val KEY_BRIGHTNESS_THRESHOLD = "brightness_threshold"
        private const val KEY_MOTION_THRESHOLD = "motion_threshold"
        private const val KEY_DETECTION_DELAY = "detection_delay"
        private const val KEY_SCREEN_OFF_DELAY = "screen_off_delay"
        private const val DEFAULT_URL = ""
        private const val CAMERA_PERMISSION_REQUEST = 1001
        private const val SETTINGS_CODE = 2001
        
        // Tap pattern for settings access (5 taps in corner)
        private const val SETTINGS_TAP_COUNT = 5
        private const val SETTINGS_TAP_TIMEOUT = 3000L
        
        // Default motion detection settings
        private const val DEFAULT_MOTION_THRESHOLD = 15.0
        private const val DEFAULT_SCREEN_OFF_DELAY = 30000L // 30 seconds
        
        // Proximity modes
        const val PROXIMITY_MODE_WAVE_WAKE = 0    // Wave to wake screen
        const val PROXIMITY_MODE_WAVE_TOGGLE = 1  // Wave to toggle screen on/off
        const val PROXIMITY_MODE_NEAR_OFF = 2     // Near = off, Far = on (like phone call)
        
        // Screen rotation modes
        const val ROTATION_PORTRAIT = 0
        const val ROTATION_LANDSCAPE = 1
        const val ROTATION_AUTO = 2
    }
    
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var landingPage: View
    private lateinit var prefs: SharedPreferences
    private lateinit var cameraExecutor: ExecutorService
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var devicePolicyManager: DevicePolicyManager? = null
    private var adminComponent: ComponentName? = null
    
    // Sensor manager for proximity
    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null
    private var hasProximitySensor = false
    
    private var isScreenOn = true
    private var lastBrightness = 255.0
    private var screenControlEnabled = true
    private var motionDetectionEnabled = true
    private var proximityEnabled = true
    private var proximityMode = PROXIMITY_MODE_WAVE_WAKE
    private var screenRotation = ROTATION_PORTRAIT
    private var brightnessThreshold = 30.0
    private var motionThreshold = DEFAULT_MOTION_THRESHOLD
    private var detectionDelay = 500L
    private var screenOffDelay = DEFAULT_SCREEN_OFF_DELAY
    private var lastToggleTime = 0L
    private var lastMotionTime = 0L
    private var lastProximityTime = 0L
    private var lastProximityNear = false
    
    // Settings tap detection
    private var settingsTapCount = 0
    private var lastTapTime = 0L
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Screen off timer runnable
    private val screenOffRunnable = Runnable {
        if (isScreenOn && (motionDetectionEnabled || proximityEnabled)) {
            Log.d(TAG, "No activity detected for ${screenOffDelay}ms, terminating display session.")
            turnScreenOff()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_main)
            
            // Setup fullscreen AFTER setting content view
            setupFullscreenMode()
            
            // Initialize components
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            cameraExecutor = Executors.newSingleThreadExecutor()
            
            // Load settings
            loadSettings()
            
            // Setup views
            setupViews()
            
            // Setup WebView
            setupWebView()
            
            // Setup device admin
            setupDeviceAdmin()
            
            // Setup wake lock
            setupWakeLock()
            
            // Setup proximity sensor
            setupProximitySensor()
            
            // Request camera permission and start camera
            if (screenControlEnabled || motionDetectionEnabled) {
                checkCameraPermission()
            }
            
            // Load home URL
            loadHomeUrl()
            
            // Start screen off timer if motion detection is enabled
            if (motionDetectionEnabled) {
                resetScreenOffTimer()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during onCreate: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun setupFullscreenMode() {
        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Fullscreen flags
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.decorView.post {
                window.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
    }
    
    private fun loadSettings() {
        // Feature disabled: Camera Cover logic removed for wall-mount optimization.
        screenControlEnabled = false
        
        motionDetectionEnabled = prefs.getBoolean(KEY_MOTION_DETECTION_ENABLED, true)
        proximityEnabled = prefs.getBoolean(KEY_PROXIMITY_ENABLED, true)
        proximityMode = prefs.getInt(KEY_PROXIMITY_MODE, PROXIMITY_MODE_WAVE_WAKE)
        screenRotation = prefs.getInt(KEY_SCREEN_ROTATION, ROTATION_PORTRAIT)
        brightnessThreshold = prefs.getFloat(KEY_BRIGHTNESS_THRESHOLD, 30f).toDouble()
        motionThreshold = prefs.getFloat(KEY_MOTION_THRESHOLD, DEFAULT_MOTION_THRESHOLD.toFloat()).toDouble()
        detectionDelay = prefs.getLong(KEY_DETECTION_DELAY, 500L)
        screenOffDelay = prefs.getLong(KEY_SCREEN_OFF_DELAY, DEFAULT_SCREEN_OFF_DELAY)
        
        // Apply screen rotation
        applyScreenRotation()
    }
    
    private fun applyScreenRotation() {
        requestedOrientation = when (screenRotation) {
            ROTATION_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            ROTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            ROTATION_AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
    
    private fun setupProximitySensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        
        hasProximitySensor = proximitySensor != null
        
        if (hasProximitySensor) {
            Log.d(TAG, "Proximity sensor found: max range = ${proximitySensor?.maximumRange}")
        } else {
            Log.d(TAG, "No proximity sensor available on this device")
        }
    }
    
    private fun registerProximitySensor() {
        if (hasProximitySensor && proximityEnabled) {
            sensorManager?.registerListener(
                this,
                proximitySensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            Log.d(TAG, "Proximity sensor registered")
        }
    }
    
    private fun unregisterProximitySensor() {
        sensorManager?.unregisterListener(this)
        Log.d(TAG, "Proximity sensor unregistered")
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_PROXIMITY) return
        if (!proximityEnabled) return
        
        val currentTime = System.currentTimeMillis()
        
        // Debounce proximity events
        if (currentTime - lastProximityTime < detectionDelay) {
            return
        }
        
        val distance = event.values[0]
        val maxRange = proximitySensor?.maximumRange ?: 5f
        val isNear = distance < maxRange
        
        Log.d(TAG, "Proximity: distance=$distance, maxRange=$maxRange, isNear=$isNear, mode=$proximityMode")
        
        // Only trigger on state change
        if (isNear == lastProximityNear) {
            return
        }
        
        lastProximityNear = isNear
        lastProximityTime = currentTime
        
        when (proximityMode) {
            PROXIMITY_MODE_WAVE_WAKE -> {
                // Wave (near then far) to wake screen
                if (!isNear && !isScreenOn) {
                    // Object moved away - wake up the screen
                    Log.d(TAG, "Proximity wave detected - waking screen")
                    turnScreenOn()
                }
            }
            
            PROXIMITY_MODE_WAVE_TOGGLE -> {
                // Wave (near then far) to toggle screen
                if (!isNear) {
                    // Object moved away - toggle screen
                    if (isScreenOn) {
                        Log.d(TAG, "Proximity wave detected - turning screen off")
                        turnScreenOff()
                    } else {
                        Log.d(TAG, "Proximity wave detected - turning screen on")
                        turnScreenOn()
                    }
                }
            }
            
            PROXIMITY_MODE_NEAR_OFF -> {
                // Near = screen off, Far = screen on (like phone call behavior)
                if (isNear && isScreenOn) {
                    Log.d(TAG, "Proximity near - turning screen off")
                    turnScreenOff()
                } else if (!isNear && !isScreenOn) {
                    Log.d(TAG, "Proximity far - turning screen on")
                    turnScreenOn()
                }
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used for proximity sensor
    }
    
    private fun setupViews() {
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        landingPage = findViewById(R.id.landingPage)
        
        // Setup landing page button
        findViewById<View>(R.id.setupButton).setOnClickListener {
            openSettings()
        }
        
        // Setup tap detection for settings access
        findViewById<View>(R.id.settingsCorner).setOnClickListener {
            handleSettingsTap()
        }
        
        // Reset motion/activity timer on touch
        val touchListener = View.OnTouchListener { _, _ ->
            resetScreenOffTimer()
            if (motionDetectionEnabled) {
                onMotionDetected()
            }
            false
        }
        webView.setOnTouchListener(touchListener)
        landingPage.setOnTouchListener(touchListener)
    }
    
    private fun handleSettingsTap() {
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - lastTapTime > SETTINGS_TAP_TIMEOUT) {
            settingsTapCount = 0
        }
        
        settingsTapCount++
        lastTapTime = currentTime
        
        if (settingsTapCount >= SETTINGS_TAP_COUNT) {
            settingsTapCount = 0
            openSettings()
        } else {
            val remaining = SETTINGS_TAP_COUNT - settingsTapCount
            Toast.makeText(this, "$remaining sequential taps required for system access.", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        intent.putExtra("has_proximity_sensor", hasProximitySensor)
        startActivityForResult(intent, SETTINGS_CODE)
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            
            // Enable caching
            cacheMode = WebSettings.LOAD_DEFAULT
            
            // User agent
            userAgentString = userAgentString + " HAKiosk/1.0"
        }
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                resetScreenOffTimer()
            }
            
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                
                // Handle external intents (tel:, mailto:, etc.)
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                        return true
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling URL: $url", e)
                    }
                }
                return false
            }
            
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    progressBar.visibility = View.GONE
                }
            }
        }
        
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
            }
            
            // Handle file chooser
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                return false
            }
            
            // Handle JavaScript alerts
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                return super.onJsAlert(view, url, message, result)
            }
        }
        
        // Enable hardware acceleration
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }
    
    private fun setupDeviceAdmin() {
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, KioskDeviceAdminReceiver::class.java)
    }
    
    @SuppressLint("WakelockTimeout")
    private fun setupWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "HAKiosk::ScreenWakeLock"
        )
    }
    
    private fun loadHomeUrl() {
        val url = prefs.getString(KEY_HOME_URL, "") ?: ""
        if (url.isEmpty()) {
            webView.visibility = View.GONE
            landingPage.visibility = View.VISIBLE
        } else {
            landingPage.visibility = View.GONE
            webView.visibility = View.VISIBLE
            webView.loadUrl(url)
        }
    }
    
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Camera authorization is necessary for motion detection functionality.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraAnalysis()
            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun bindCameraAnalysis() {
        val cameraProvider = cameraProvider ?: return
        
        // Build image analyzer with motion detection
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(android.util.Size(320, 240))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor, MotionBrightnessAnalyzer(
                    motionThreshold = motionThreshold,
                    onBrightnessChange = { brightness ->
                        if (screenControlEnabled) {
                            handleBrightnessChange(brightness)
                        }
                    },
                    onMotionDetected = { motionLevel ->
                        if (motionDetectionEnabled) {
                            handleMotionDetected(motionLevel)
                        }
                    }
                ))
            }
        
        // Use front camera
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()
        
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalyzer)
            Log.d(TAG, "Camera bound successfully for motion and brightness analysis")
        } catch (e: Exception) {
            Log.e(TAG, "Camera binding failed", e)
        }
    }
    
    private fun handleBrightnessChange(brightness: Double) {
        // Feature disabled: Camera Cover logic removed for wall-mount optimization.
        lastBrightness = brightness
    }
    
    private fun handleMotionDetected(motionLevel: Double) {
        if (motionLevel > motionThreshold) {
            Log.d(TAG, "Motion detected: $motionLevel (threshold: $motionThreshold)")
            onMotionDetected()
        }
    }
    
    private fun onMotionDetected() {
        val currentTime = System.currentTimeMillis()
        lastMotionTime = currentTime
        
        // If screen is off, turn it on
        if (!isScreenOn) {
            mainHandler.post {
                Log.d(TAG, "Motion detected, turning screen on")
                turnScreenOn()
            }
        }
        
        // Reset the screen off timer
        resetScreenOffTimer()
    }
    
    private fun resetScreenOffTimer() {
        mainHandler.removeCallbacks(screenOffRunnable)
        if (isScreenOn && (motionDetectionEnabled || proximityEnabled)) {
            mainHandler.postDelayed(screenOffRunnable, screenOffDelay)
        }
    }
    
    private fun turnScreenOff() {
        mainHandler.post {
            if (!isScreenOn) return@post
            
            isScreenOn = false
            Log.d(TAG, "Turning screen OFF")
            
            // Cancel the screen off timer
            mainHandler.removeCallbacks(screenOffRunnable)
            
            // Dim the screen to minimum
            val params = window.attributes
            params.screenBrightness = 0.01f
            window.attributes = params
            
            // Hide the WebView
            webView.visibility = View.INVISIBLE
            
            // Try to lock screen if we have device admin
            if (devicePolicyManager?.isAdminActive(adminComponent!!) == true) {
                try {
                    devicePolicyManager?.lockNow()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to lock screen", e)
                }
            }
        }
    }
    
    private fun turnScreenOn() {
        mainHandler.post {
            if (isScreenOn) return@post
            
            isScreenOn = true
            Log.d(TAG, "Turning screen ON")
            
            // Restore screen brightness
            val params = window.attributes
            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = params
            
            // Show the WebView
            webView.visibility = View.VISIBLE
            
            // Acquire wake lock to turn on screen
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(1000)
            }
            
            // Reset screen off timer
            if (motionDetectionEnabled) {
                resetScreenOffTimer()
            }
        }
    }
    
    // Combined Motion and Brightness Analyzer
    private class MotionBrightnessAnalyzer(
        private val motionThreshold: Double,
        private val onBrightnessChange: (Double) -> Unit,
        private val onMotionDetected: (Double) -> Unit
    ) : ImageAnalysis.Analyzer {
        
        private var previousFrame: ByteArray? = null
        private var frameCount = 0
        private val analyzeEveryNFrames = 3 // Analyze every 3rd frame for performance
        
        // Grid-based motion detection (divide frame into regions)
        private val gridRows = 8
        private val gridCols = 8
        private var previousGridValues: DoubleArray? = null
        
        override fun analyze(image: ImageProxy) {
            frameCount++
            
            // Process every Nth frame for performance
            if (frameCount % analyzeEveryNFrames != 0) {
                image.close()
                return
            }
            
            val buffer: ByteBuffer = image.planes[0].buffer
            val data = ByteArray(buffer.remaining())
            buffer.rewind()
            buffer.get(data)
            
            // Calculate brightness
            val brightness = calculateAverageBrightness(data)
            onBrightnessChange(brightness)
            
            // Calculate motion
            val motionLevel = calculateMotionLevel(data, image.width, image.height)
            if (motionLevel > 0) {
                onMotionDetected(motionLevel)
            }
            
            // Store current frame for next comparison
            previousFrame = data.clone()
            
            image.close()
        }
        
        private fun calculateAverageBrightness(data: ByteArray): Double {
            var sum = 0L
            // Sample every 4th pixel for performance
            for (i in data.indices step 4) {
                sum += (data[i].toInt() and 0xFF)
            }
            return (sum.toDouble() / (data.size / 4))
        }
        
        private fun calculateMotionLevel(currentFrame: ByteArray, width: Int, height: Int): Double {
            val prevFrame = previousFrame ?: return 0.0
            
            if (prevFrame.size != currentFrame.size) {
                return 0.0
            }
            
            // Grid-based motion detection for better accuracy
            val currentGridValues = calculateGridValues(currentFrame, width, height)
            val prevGridValues = previousGridValues
            
            previousGridValues = currentGridValues
            
            if (prevGridValues == null) {
                return 0.0
            }
            
            // Calculate motion as average absolute difference between grid cells
            var totalDiff = 0.0
            var significantCells = 0
            
            for (i in currentGridValues.indices) {
                val diff = abs(currentGridValues[i] - prevGridValues[i])
                if (diff > 5) { // Minimum threshold per cell to filter noise
                    totalDiff += diff
                    significantCells++
                }
            }
            
            // Motion level is based on how many cells changed and by how much
            val motionLevel = if (significantCells > 0) {
                (totalDiff / significantCells) * (significantCells.toDouble() / (gridRows * gridCols))
            } else {
                0.0
            }
            
            return motionLevel * 10 // Scale up for easier threshold configuration
        }
        
        private fun calculateGridValues(frame: ByteArray, width: Int, height: Int): DoubleArray {
            val gridValues = DoubleArray(gridRows * gridCols)
            val cellWidth = width / gridCols
            val cellHeight = height / gridRows
            
            for (row in 0 until gridRows) {
                for (col in 0 until gridCols) {
                    var sum = 0L
                    var count = 0
                    
                    val startY = row * cellHeight
                    val endY = minOf((row + 1) * cellHeight, height)
                    val startX = col * cellWidth
                    val endX = minOf((col + 1) * cellWidth, width)
                    
                    // Sample pixels in this cell
                    for (y in startY until endY step 4) {
                        for (x in startX until endX step 4) {
                            val index = y * width + x
                            if (index < frame.size) {
                                sum += (frame[index].toInt() and 0xFF)
                                count++
                            }
                        }
                    }
                    
                    gridValues[row * gridCols + col] = if (count > 0) sum.toDouble() / count else 0.0
                }
            }
            
            return gridValues
        }
    }
    
    // Prevent back button from exiting
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        }
        // Don't call super.onBackPressed() to prevent exit
    }
    
    // Prevent home and recent apps buttons
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_APP_SWITCH -> true
            else -> super.onKeyDown(keyCode, event)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == SETTINGS_CODE) {
            // Reload settings
            loadSettings()
            
            // Restart camera if screen control settings changed
            if (screenControlEnabled || motionDetectionEnabled) {
                checkCameraPermission()
            } else {
                stopCamera()
            }
            
            // Re-register proximity sensor with new settings
            unregisterProximitySensor()
            if (proximityEnabled) {
                registerProximitySensor()
            }
            
            // Reset screen off timer if motion detection is enabled
            if (motionDetectionEnabled) {
                resetScreenOffTimer()
            } else {
                mainHandler.removeCallbacks(screenOffRunnable)
            }
            
            // Reload URL if changed
            if (resultCode == RESULT_OK) {
                loadHomeUrl()
            }
        }
    }
    
    private fun stopCamera() {
        cameraProvider?.unbindAll()
    }
    
    override fun onResume() {
        super.onResume()
        setupFullscreenMode()
        
        // Register proximity sensor
        if (proximityEnabled) {
            registerProximitySensor()
        }
        
        if ((screenControlEnabled || motionDetectionEnabled) && cameraProvider == null) {
            checkCameraPermission()
        }
        
        // Reset motion timer on resume
        if (motionDetectionEnabled) {
            onMotionDetected()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Unregister proximity sensor to save battery
        unregisterProximitySensor()
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setupFullscreenMode()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacks(screenOffRunnable)
        unregisterProximitySensor()
        cameraExecutor.shutdown()
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }
}
