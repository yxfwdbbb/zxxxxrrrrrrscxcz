package com.lts.control.core.ble

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lts.control.core.ble.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

class BleViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "BleViewModel"
    }
    
    // BLE Manager
    private val bleManager = BleManager(application.applicationContext)
    
    // UI States
    private val _connection = MutableStateFlow<BleManager.ConnectionState>(BleManager.ConnectionState.Disconnected)
    val connection: StateFlow<BleManager.ConnectionState> = _connection
    
    private val _status = MutableStateFlow<DeviceStatus?>(null)
    val status: StateFlow<DeviceStatus?> = _status
    
    private val _deviceState = MutableStateFlow(DeviceState.IDLE)
    val deviceState: StateFlow<DeviceState> = _deviceState
    
    private val _progressBarValue = MutableStateFlow(0f)
    val progressBarValue: StateFlow<Float> = _progressBarValue
    
    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds
    
    private val _highSpeed = MutableStateFlow(false)
    val highSpeed: StateFlow<Boolean> = _highSpeed
    
    // Messages from device
    private val _messages = MutableSharedFlow<IncomingMessage>()
    val messages = _messages.asSharedFlow()
    
    // Internal states
    private var isCalibrating = false
    private var calibrationStep = 0
    
    init {
        observeBleEvents()
    }
    
    private fun observeBleEvents() {
        viewModelScope.launch {
            bleManager.connectionState.collect { state ->
                _connection.value = state
                when (state) {
                    is BleManager.ConnectionState.Connected -> {
                        requestInitialStatus()
                    }
                    is BleManager.ConnectionState.Disconnected -> {
                        resetStates()
                    }
                    else -> {}
                }
            }
        }
        
        viewModelScope.launch {
            bleManager.incomingMessages.collect { message ->
                handleIncomingMessage(message)
            }
        }
    }
    
    private fun requestInitialStatus() {
        viewModelScope.launch {
            delay(500)
            sendCommand("GET_STATUS")
            delay(500)
            sendCommand("GET_CONFIG")
        }
    }
    
    private fun resetStates() {
        _status.value = null
        _deviceState.value = DeviceState.IDLE
        _progressBarValue.value = 0f
        _remainingSeconds.value = 0
        _highSpeed.value = false
    }
    
    private fun handleIncomingMessage(message: IncomingMessage) {
        viewModelScope.launch {
            when (message) {
                is IncomingMessage.StatusUpdate -> {
                    updateStatus(message.status)
                }
                is IncomingMessage.ConfigUpdate -> {
                    updateConfig(message.config)
                }
                is IncomingMessage.StateChange -> {
                    _deviceState.value = message.state
                }
                is IncomingMessage.ProgressUpdate -> {
                    _progressBarValue.value = message.progress
                    _remainingSeconds.value = message.remainingSeconds
                }
                is IncomingMessage.WifiScan -> {
                    _messages.emit(message)
                }
                is IncomingMessage.WifiConnectResult -> {
                    _messages.emit(message)
                }
                else -> {}
            }
        }
    }
    
    private fun updateStatus(status: DeviceStatus) {
        _status.value = status
        
        // Update derived states
        _highSpeed.value = status.highSpeed == true
    }
    
    private fun updateConfig(config: DeviceConfig) {
        // Handle config updates if needed
    }
    
    // ==================== BLE Connection ====================
    
    fun startScan() {
        bleManager.startScan()
    }
    
    fun stopScan() {
        bleManager.stopScan()
    }
    
    fun connect(address: String) {
        bleManager.connect(address)
    }
    
    fun disconnect() {
        bleManager.disconnect()
    }
    
    // ==================== Device Control ====================
    
    fun start() {
        sendCommand("START")
    }
    
    fun pause() {
        sendCommand("PAUSE")
    }
    
    fun stop() {
        sendCommand("STOP")
    }
    
    fun setSpeed(speed: Int) {
        sendCommand("SET_SPEED:$speed")
    }
    
    fun setDirection(direction: Int) {
        sendCommand("SET_DIRECTION:$direction")
    }
    
    fun setMotorStrength(strength: Int) {
        sendCommand("SET_STRENGTH:$strength")
    }
    
    fun setTorque(level: Int) {
        sendCommand("SET_TORQUE:$level")
    }
    
    fun setHighSpeed(enabled: Boolean) {
        sendCommand("SET_HIGHSPEED:${if (enabled) "1" else "0"}")
    }
    
    fun setLed(brightness: Int) {
        sendCommand("SET_LED:$brightness")
    }
    
    fun setFanSpeed(speed: Int) {
        sendCommand("SET_FAN:$speed")
    }
    
    fun setFanAlways(enabled: Boolean) {
        sendCommand("SET_FAN_ALWAYS:${if (enabled) "1" else "0"}")
    }
    
    fun setJingle(style: Int) {
        sendCommand("SET_JINGLE:$style")
    }
    
    fun setFilamentSensor(enabled: Boolean) {
        sendCommand("SET_FILAMENT_SENSOR:${if (enabled) "1" else "0"}")
    }
    
    fun setTargetWeight(weight: Int) {
        sendCommand("SET_TARGET_WEIGHT:$weight")
    }
    
    fun setDurationAt80(duration: Int) {
        sendCommand("SET_DURATION_80:$duration")
    }
    
    // ==================== WiFi Control ====================
    
    fun wifiScan() {
        sendCommand("WIFI_SCAN")
    }
    
    fun wifiConnect(ssid: String, pass: String) {
        sendCommand("WIFI_CONNECT:$ssid,$pass")
    }
    
    // ==================== Servo Calibration ====================
    
    /**
     * 开始舵机校准流程
     * 对应网页版：舵机校准按钮
     */
    fun calibrateServo() {
        Log.d(TAG, "Starting servo calibration")
        sendCommand("CALIBRATE_SERVO")
    }
    
    /**
     * 设置舵机到左侧极限位置
     * 对应网页版：左侧按钮
     */
    fun setServoLeft() {
        Log.d(TAG, "Setting servo to left position")
        sendCommand("SERVO_LEFT")
    }
    
    /**
     * 设置舵机到右侧极限位置
     * 对应网页版：右侧按钮
     */
    fun setServoRight() {
        Log.d(TAG, "Setting servo to right position")
        sendCommand("SERVO_RIGHT")
    }
    
    /**
     * 请求舵机当前角度
     * 用于实时显示角度值
     */
    fun requestServoAngle() {
        sendCommand("GET_SERVO_ANGLE")
    }
    
    /**
     * 保存舵机校准参数
     */
    fun saveServoCalibration() {
        sendCommand("SAVE_SERVO_CALIBRATION")
    }
    
    /**
     * 重置舵机校准参数
     */
    fun resetServoCalibration() {
        sendCommand("RESET_SERVO_CALIBRATION")
    }
    
    // ==================== Helper Methods ====================
    
    private fun sendCommand(command: String) {
        viewModelScope.launch {
            bleManager.sendCommand(command)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        bleManager.release()
    }
}
