package com.example.flipperdroid.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel gerant la detection et le scan des appareils bluetooth
 *
 * Cette classe gere les permissions necessaires à l'initialisation de l adaptateur bluetooth et du scanner BLE
 * Elle expose les listes des appareils detectes l'état du scan et si les permissions sont accordees via des StateFlow
 */
class BluetoothViewModel : ViewModel() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanning = false

    @SuppressLint("StaticFieldLeak")
    private var context: Context? = null

    private val _devices = MutableStateFlow<List<ScanResult>>(emptyList())

    private val _isScanning = MutableStateFlow(false)

    private val _permissionsGranted = MutableStateFlow(false)


    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val currentList = _devices.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.device.address == result.device.address }
            
            if (existingIndex >= 0) {
                currentList[existingIndex] = result
            } else {
                currentList.add(result)
            }
            
            viewModelScope.launch {
                _devices.emit(currentList)
            }
        }
    }
    /**
     * Initialise le viewmodel avec le contexte et configure l'adaptateur et le scanner bluetooth
     *
     * @param context contexte de l application necessaire pour acceder aux services systeme
     */

    fun initialize(context: Context) {
        this.context = context
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        checkPermissions()
    }
    /**
     * Verifie si les permissions bluetooth necessaires sont accordees
     *
     * Cette methode permet de savoir les permissions de permissionsGranted
     */
    private fun checkPermissions() {
        context?.let { ctx ->
            val hasBluetoothPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
            }
            viewModelScope.launch {
                _permissionsGranted.emit(hasBluetoothPermission)
            }
        }
    }


    /**
     * Arrete le scan bluetooth si il est en cours
     *
     * Cette methode met à jour l'état du scan et intercepte les exceptions de sécurité pour les permissions
     */
    fun stopScan() {
        if (scanning && bluetoothLeScanner != null) {
            try {
                scanning = false
                bluetoothLeScanner?.stopScan(scanCallback)
                viewModelScope.launch {
                    _isScanning.emit(false)
                }
            } catch (e: SecurityException) {
                viewModelScope.launch {
                    _permissionsGranted.emit(false)
                }
            }
        }
    }


    /**
     * Methode appelée lors de la suppression du ViewModel afin de l'arreter proprement le scan bluetooth
     */
    override fun onCleared() {
        super.onCleared()
        stopScan()
    }
} 