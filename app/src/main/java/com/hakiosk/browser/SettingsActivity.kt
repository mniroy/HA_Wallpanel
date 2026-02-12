package com.hakiosk.browser

import androidx.appcompat.app.AppCompatDelegate
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    
    companion object {
        private const val PREFS_NAME = "HAWallpanelPrefs"
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
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_HIDE_HEADER = "hide_header"
        private const val KEY_HIDE_SIDEBAR = "hide_sidebar"
        private const val KEY_PIN_CODE = "pin_code"

        private const val KEY_CAMERA_STREAM_ENABLED = "camera_stream_enabled"
        private const val DEFAULT_URL = ""
        private const val DEVICE_ADMIN_REQUEST = 1001
        
        // Proximity modes - must match MainActivity
        const val PROXIMITY_MODE_WAVE_WAKE = 0
        const val PROXIMITY_MODE_WAVE_TOGGLE = 1
        const val PROXIMITY_MODE_NEAR_OFF = 2
        
        // Screen rotation modes - must match MainActivity
        const val ROTATION_PORTRAIT = 0
        const val ROTATION_LANDSCAPE = 1
        const val ROTATION_AUTO = 2
    }
    
    private lateinit var prefs: SharedPreferences
    
    private lateinit var urlInput: EditText
    private lateinit var motionDetectionSwitch: Switch
    private lateinit var hideHeaderSwitch: Switch
    private lateinit var hideSidebarSwitch: Switch
    private lateinit var proximitySwitch: Switch
    private lateinit var proximityModeSpinner: Spinner
    private lateinit var proximityModeContainer: LinearLayout
    private lateinit var proximitySensorStatus: TextView
    
    private lateinit var cameraStreamSwitch: Switch
    private lateinit var streamUrlContainer: LinearLayout
    private lateinit var streamUrlValue: TextView
    
    private lateinit var themeSpinner: Spinner

    private lateinit var screenRotationSpinner: Spinner
    private lateinit var brightnessSeekBar: SeekBar
    private lateinit var brightnessValue: TextView
    private lateinit var motionSeekBar: SeekBar
    private lateinit var motionValue: TextView
    private lateinit var delaySeekBar: SeekBar
    private lateinit var delayValue: TextView
    private lateinit var screenOffSeekBar: SeekBar
    private lateinit var screenOffValue: TextView
    private lateinit var pinInput: EditText
    private lateinit var saveButton: Button
    private lateinit var enableAdminButton: Button
    private lateinit var exitKioskButton: Button
    
    private var hasProximitySensor = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        hasProximitySensor = intent.getBooleanExtra("has_proximity_sensor", false)
        
        setupViews()
        loadSettings()
    }
    
    private fun setupViews() {
        urlInput = findViewById(R.id.urlInput)
        motionDetectionSwitch = findViewById(R.id.motionDetectionSwitch)
        hideHeaderSwitch = findViewById(R.id.hideHeaderSwitch)
        hideSidebarSwitch = findViewById(R.id.hideSidebarSwitch)
        proximitySwitch = findViewById(R.id.proximitySwitch)
        proximityModeSpinner = findViewById(R.id.proximityModeSpinner)
        proximityModeContainer = findViewById(R.id.proximityModeContainer)
        proximityModeContainer = findViewById(R.id.proximityModeContainer)
        proximitySensorStatus = findViewById(R.id.proximitySensorStatus)
        
        cameraStreamSwitch = findViewById(R.id.cameraStreamSwitch)
        streamUrlContainer = findViewById(R.id.streamUrlContainer)
        streamUrlValue = findViewById(R.id.streamUrlValue)
        
        themeSpinner = findViewById(R.id.themeSpinner)
        screenRotationSpinner = findViewById(R.id.screenRotationSpinner)
        brightnessSeekBar = findViewById(R.id.brightnessSeekBar)
        brightnessValue = findViewById(R.id.brightnessValue)
        motionSeekBar = findViewById(R.id.motionSeekBar)
        motionValue = findViewById(R.id.motionValue)
        delaySeekBar = findViewById(R.id.delaySeekBar)
        delayValue = findViewById(R.id.delayValue)
        screenOffSeekBar = findViewById(R.id.screenOffSeekBar)
        screenOffValue = findViewById(R.id.screenOffValue)
        pinInput = findViewById(R.id.pinInput)
        saveButton = findViewById(R.id.saveButton)
        enableAdminButton = findViewById(R.id.enableAdminButton)
        exitKioskButton = findViewById(R.id.exitKioskButton)
        
        // Setup proximity sensor status
        if (hasProximitySensor) {
            proximitySensorStatus.text = "Hardware state: Active ✓"
            proximitySensorStatus.setTextColor(getColor(R.color.success))
        } else {
            proximitySensorStatus.text = "Hardware state: Not detected"
            proximitySensorStatus.setTextColor(getColor(R.color.error))
            proximitySwitch.isEnabled = false
            proximityModeSpinner.isEnabled = false
        }
        
        // Setup theme spinner
        val themeModes = arrayOf(
            "System Default",
            "Light",
            "Dark"
        )
        val themeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themeModes)
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        themeSpinner.adapter = themeAdapter
        
        // Setup proximity mode spinner
        val proximityModes = arrayOf(
            "Detection wake (active gesture)",
            "Toggle display (active gesture)",
            "Dynamic occupancy (near/far logic)"
        )
        val proximityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, proximityModes)
        proximityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        proximityModeSpinner.adapter = proximityAdapter
        
        // Setup screen rotation spinner
        val rotationModes = arrayOf(
            "Locked: Portrait",
            "Locked: Landscape",
            "Dynamic: Sensor based"
        )
        val rotationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, rotationModes)
        rotationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        screenRotationSpinner.adapter = rotationAdapter
        
        // Show/hide proximity mode based on switch
        proximitySwitch.setOnCheckedChangeListener { _, isChecked ->
            proximityModeContainer.visibility = if (isChecked && hasProximitySensor) View.VISIBLE else View.GONE
        }
        
        // Show/hide stream URL based on switch
        cameraStreamSwitch.setOnCheckedChangeListener { _, isChecked ->
            streamUrlContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked) {
                updateStreamUrl()
            }
        }
        
        // Brightness threshold seekbar (0-100)
        brightnessSeekBar.max = 100
        brightnessSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                brightnessValue.text = "Luminance Threshold: $progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Motion sensitivity seekbar (1-50)
        motionSeekBar.max = 49
        motionSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val sensitivity = progress + 1
                motionValue.text = "Detection Intensity: $sensitivity"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Detection delay seekbar (100-2000ms)
        delaySeekBar.max = 1900
        delaySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val delay = progress + 100
                delayValue.text = "Response Latency: ${delay}ms"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Screen off delay seekbar (5-300 seconds)
        screenOffSeekBar.max = 295
        screenOffSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val seconds = progress + 5
                val displayText = if (seconds >= 60) {
                    val minutes = seconds / 60
                    val remainingSecs = seconds % 60
                    if (remainingSecs > 0) {
                        "Turn screen off after: ${minutes}m ${remainingSecs}s"
                    } else {
                        "Turn screen off after: ${minutes}m"
                    }
                } else {
                    "Turn screen off after: ${seconds}s"
                }
                screenOffValue.text = displayText
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        saveButton.setOnClickListener {
            saveSettings()
        }
        
        enableAdminButton.setOnClickListener {
            enableDeviceAdmin()
        }
        
        exitKioskButton.setOnClickListener {
            showExitConfirmation()
        }
        
        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }
    }
    
    private fun loadSettings() {
        urlInput.setText(prefs.getString(KEY_HOME_URL, DEFAULT_URL))
        motionDetectionSwitch.isChecked = prefs.getBoolean(KEY_MOTION_DETECTION_ENABLED, true)
        hideHeaderSwitch.isChecked = prefs.getBoolean(KEY_HIDE_HEADER, false)
        hideSidebarSwitch.isChecked = prefs.getBoolean(KEY_HIDE_SIDEBAR, false)
        proximitySwitch.isChecked = prefs.getBoolean(KEY_PROXIMITY_ENABLED, true)
        
        val streamEnabled = prefs.getBoolean(KEY_CAMERA_STREAM_ENABLED, false)
        cameraStreamSwitch.isChecked = streamEnabled
        streamUrlContainer.visibility = if (streamEnabled) View.VISIBLE else View.GONE
        if (streamEnabled) {
            updateStreamUrl()
        }
        
        val proximityMode = prefs.getInt(KEY_PROXIMITY_MODE, PROXIMITY_MODE_WAVE_WAKE)
        proximityModeSpinner.setSelection(proximityMode)
        
        val appTheme = prefs.getInt(KEY_APP_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        themeSpinner.setSelection(when(appTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> 1
            AppCompatDelegate.MODE_NIGHT_YES -> 2
            else -> 0
        })
        
        val screenRotation = prefs.getInt(KEY_SCREEN_ROTATION, ROTATION_PORTRAIT)
        screenRotationSpinner.setSelection(screenRotation)
        
        // Show/hide proximity mode container
        proximityModeContainer.visibility = if (proximitySwitch.isChecked && hasProximitySensor) View.VISIBLE else View.GONE
        
        val brightness = prefs.getFloat(KEY_BRIGHTNESS_THRESHOLD, 30f).toInt()
        brightnessSeekBar.progress = brightness
        brightnessValue.text = "Luminance Threshold: $brightness"
        
        // Motion threshold is inverted for sensitivity (higher sensitivity = lower threshold)
        val motionThreshold = prefs.getFloat(KEY_MOTION_THRESHOLD, 15f).toInt()
        val motionSensitivity = 51 - motionThreshold // Invert: threshold 1-50 -> sensitivity 50-1
        motionSeekBar.progress = motionSensitivity.coerceIn(0, 49)
        motionValue.text = "Detection Intensity: ${motionSensitivity + 1}"
        
        val delay = prefs.getLong(KEY_DETECTION_DELAY, 500L).toInt()
        delaySeekBar.progress = delay - 100
        delayValue.text = "Response Latency: ${delay}ms"
        
        val screenOffDelayMs = prefs.getLong(KEY_SCREEN_OFF_DELAY, 30000L)
        val screenOffSeconds = (screenOffDelayMs / 1000).toInt()
        screenOffSeekBar.progress = (screenOffSeconds - 5).coerceIn(0, 295)
        val displayText = if (screenOffSeconds >= 60) {
            val minutes = screenOffSeconds / 60
            val remainingSecs = screenOffSeconds % 60
            if (remainingSecs > 0) {
                "Turn screen off after: ${minutes}m ${remainingSecs}s"
            } else {
                "Turn screen off after: ${minutes}m"
            }
        } else {
            "Turn screen off after: ${screenOffSeconds}s"
        }
        screenOffValue.text = displayText
        
        pinInput.setText(prefs.getString(KEY_PIN_CODE, "1234"))
        
        updateDeviceAdminStatus()
    }
    
    private fun saveSettings() {
        val url = urlInput.text.toString().trim()
        // Allow empty URL to show landing page
        val finalUrl = if (url.isNotEmpty() && !url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }
        
        // Convert sensitivity to threshold (invert)
        val motionSensitivity = motionSeekBar.progress + 1
        val motionThreshold = 51 - motionSensitivity
        
        // Calculate screen off delay in milliseconds
        val screenOffSeconds = screenOffSeekBar.progress + 5
        val screenOffDelayMs = screenOffSeconds * 1000L
        
        val selectedTheme = when(themeSpinner.selectedItemPosition) {
            1 -> AppCompatDelegate.MODE_NIGHT_NO
            2 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        
        prefs.edit().apply {
            putString(KEY_HOME_URL, finalUrl)
            putBoolean(KEY_MOTION_DETECTION_ENABLED, motionDetectionSwitch.isChecked)
            putBoolean(KEY_HIDE_HEADER, hideHeaderSwitch.isChecked)
            putBoolean(KEY_HIDE_SIDEBAR, hideSidebarSwitch.isChecked)
            putBoolean(KEY_CAMERA_STREAM_ENABLED, cameraStreamSwitch.isChecked)
            putBoolean(KEY_PROXIMITY_ENABLED, proximitySwitch.isChecked)
            putInt(KEY_PROXIMITY_MODE, proximityModeSpinner.selectedItemPosition)
            putInt(KEY_SCREEN_ROTATION, screenRotationSpinner.selectedItemPosition)
            putFloat(KEY_BRIGHTNESS_THRESHOLD, brightnessSeekBar.progress.toFloat())
            putFloat(KEY_MOTION_THRESHOLD, motionThreshold.toFloat())
            putLong(KEY_DETECTION_DELAY, (delaySeekBar.progress + 100).toLong())
            putLong(KEY_SCREEN_OFF_DELAY, screenOffDelayMs)
            putInt(KEY_APP_THEME, selectedTheme)
            putString(KEY_PIN_CODE, pinInput.text.toString())
            apply()
        }
        
        AppCompatDelegate.setDefaultNightMode(selectedTheme)
        
        Toast.makeText(this, "Configuration Applied", Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }
    
    private fun enableDeviceAdmin() {
        val adminComponent = ComponentName(this, KioskDeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Grant administrative authority to manage device lock states."
            )
        }
        startActivityForResult(intent, DEVICE_ADMIN_REQUEST)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == DEVICE_ADMIN_REQUEST) {
            updateDeviceAdminStatus()
        }
    }
    
    private fun updateDeviceAdminStatus() {
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, KioskDeviceAdminReceiver::class.java)
        
        val isAdmin = devicePolicyManager.isAdminActive(adminComponent)
        enableAdminButton.text = if (isAdmin) "Administrative Authority: Active ✓" else "Grant Administrative Authority"
        enableAdminButton.isEnabled = !isAdmin
    }
    
    private fun showExitConfirmation() {
        val savedPin = prefs.getString(KEY_PIN_CODE, "1234") ?: "1234"
        
        val pinInputDialog = EditText(this).apply {
            hint = "Security Code"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or 
                       android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        
        AlertDialog.Builder(this)
            .setTitle("Terminate Application")
            .setMessage("Enter the security code to terminate the current session.")
            .setView(pinInputDialog)
            .setPositiveButton("Terminate") { _, _ ->
                if (pinInputDialog.text.toString() == savedPin) {
                    finishAffinity()
                } else {
                    Toast.makeText(this, "Invalid security code.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Discard", null)
            .show()
    }
    
    private fun updateStreamUrl() {
        val ip = getIpAddress()
        streamUrlValue.text = "http://$ip:2971/"
    }
    
    private fun getIpAddress(): String {
        try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipAddress = wifiManager.connectionInfo.ipAddress
            return String.format(
                "%d.%d.%d.%d",
                (ipAddress and 0xff),
                (ipAddress shr 8 and 0xff),
                (ipAddress shr 16 and 0xff),
                (ipAddress shr 24 and 0xff)
            )
        } catch (e: Exception) {
            return "0.0.0.0"
        }
    }
}
