package com.example.flipperdroid.model.`class`

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.example.flipperdroid.model.enums.AdvertisementError
import com.example.flipperdroid.model.enums.TxPowerLevel
import com.example.flipperdroid.model.`interface`.IAdvertisementService
import com.example.flipperdroid.model.`interface`.IAdvertisementServiceCallback
import com.example.flipperdroid.model.`object`.BluetoothHelpers.bluetoothAdapter

class ModernAdvertisementService(
    private val context: Context,
): IAdvertisementService {
    private val _logTag = "ModernAdvertisementService"
    private var _bluetoothAdapter: BluetoothAdapter? = null
    private var _advertiser: BluetoothLeAdvertiser? = null
    private var _advertisementServiceCallbacks: MutableList<IAdvertisementServiceCallback> = mutableListOf()
    private var _currentAdvertisementSet: AdvertisementSet? = null
    private var _txPowerLevel: TxPowerLevel? = null

    init {
        _bluetoothAdapter = context.bluetoothAdapter()
        if (_bluetoothAdapter != null) {
            _advertiser = _bluetoothAdapter!!.bluetoothLeAdvertiser
        }
    }

    fun prepareAdvertisementSet(advertisementSet: AdvertisementSet): AdvertisementSet {
        if (_txPowerLevel != null) {
            advertisementSet.advertiseSettings.txPowerLevel = _txPowerLevel!!
            advertisementSet.advertisingSetParameters.txPowerLevel = _txPowerLevel!!
        }
        advertisementSet.advertisingSetCallback = getAdvertisingSetCallback()
        return advertisementSet
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    override fun startAdvertisement(advertisementSet: AdvertisementSet) {
        if (_advertiser != null) {
            if (advertisementSet.validate()) {
                val preparedAdvertisementSet = prepareAdvertisementSet(advertisementSet)
                if (preparedAdvertisementSet.scanResponse != null) {
                    _advertiser!!.startAdvertisingSet(
                        preparedAdvertisementSet.advertisingSetParameters.build(),
                        preparedAdvertisementSet.advertiseData.build(),
                        preparedAdvertisementSet.scanResponse!!.build(),
                        null, null, preparedAdvertisementSet.advertisingSetCallback
                    )
                } else {
                    _advertiser!!.startAdvertisingSet(
                        preparedAdvertisementSet.advertisingSetParameters.build(),
                        preparedAdvertisementSet.advertiseData.build(),
                        null, null, null, preparedAdvertisementSet.advertisingSetCallback
                    )
                }
                Log.d(_logTag, "Started Modern Advertisement")
                _currentAdvertisementSet = preparedAdvertisementSet
                _advertisementServiceCallbacks.map {
                    it.onAdvertisementSetStart(advertisementSet)
                }
            } else {
                Log.d(_logTag, "Advertisement Set could not be validated")
            }
        } else {
            Log.d(_logTag, "Advertiser is null")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    override fun stopAdvertisement() {
        if (_advertiser != null) {
            if (_currentAdvertisementSet != null) {
                _advertiser!!.stopAdvertisingSet(_currentAdvertisementSet!!.advertisingSetCallback)
                _currentAdvertisementSet = null
            } else {
                Log.d(_logTag, "Current Modern Advertising Set is null")
            }
        } else {
            Log.d(_logTag, "Advertiser is null")
        }
    }

    override fun setTxPowerLevel(txPowerLevel: TxPowerLevel) {
        _txPowerLevel = txPowerLevel
    }

    override fun getTxPowerLevel(): TxPowerLevel {
        return _txPowerLevel ?: TxPowerLevel.TX_POWER_HIGH
    }

    override fun addAdvertisementServiceCallback(callback: IAdvertisementServiceCallback) {
        if (!_advertisementServiceCallbacks.contains(callback)) {
            _advertisementServiceCallbacks.add(callback)
        }
    }
    override fun removeAdvertisementServiceCallback(callback: IAdvertisementServiceCallback) {
        if (_advertisementServiceCallbacks.contains(callback)) {
            _advertisementServiceCallbacks.remove(callback)
        }
    }

    override fun isLegacyService(): Boolean {
        return false
    }

    private fun getAdvertisingSetCallback(): AdvertisingSetCallback {
        return object : AdvertisingSetCallback() {
            override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
                if (status == ADVERTISE_SUCCESS) {
                    _advertisementServiceCallbacks.map {
                        it.onAdvertisementSetSucceeded(_currentAdvertisementSet)
                    }
                } else {
                    val advertisementError = when (status) {
                        ADVERTISE_FAILED_ALREADY_STARTED -> AdvertisementError.ADVERTISE_FAILED_ALREADY_STARTED
                        ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> AdvertisementError.ADVERTISE_FAILED_FEATURE_UNSUPPORTED
                        ADVERTISE_FAILED_INTERNAL_ERROR -> AdvertisementError.ADVERTISE_FAILED_INTERNAL_ERROR
                        ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> AdvertisementError.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS
                        ADVERTISE_FAILED_DATA_TOO_LARGE -> AdvertisementError.ADVERTISE_FAILED_DATA_TOO_LARGE
                        else -> AdvertisementError.ADVERTISE_FAILED_UNKNOWN
                    }
                    _advertisementServiceCallbacks.map {
                        it.onAdvertisementSetFailed(_currentAdvertisementSet, advertisementError)
                    }
                }
            }
            override fun onAdvertisingDataSet(advertisingSet: AdvertisingSet, status: Int) {}
            override fun onScanResponseDataSet(advertisingSet: AdvertisingSet, status: Int) {}
            override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet) {
                _advertisementServiceCallbacks.map {
                    it.onAdvertisementSetStop(_currentAdvertisementSet)
                }
            }
        }
    }
}