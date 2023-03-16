package com.chen.communicateonbt

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread
import kotlin.experimental.and

@SuppressLint("StaticFieldLeak")
object ECBLE {

    var bleContext: Context? = null
    var bleActivity: Activity? = null
    var bluetoothAdapter: BluetoothAdapter? = null
    var leScanCallback =
        BluetoothAdapter.LeScanCallback { bluetoothDevice: BluetoothDevice, rssi: Int, bytes: ByteArray ->
//            Log.e("bleDiscovery", bluetoothDevice.name + "|" + rssi)
//            Log.e("bleDiscovery-bytes-len", "" + bytes.size)
//            Log.e("bleDiscovery-bytes", "" + bytesToHexString(bytes))
            if (bluetoothDevice.name == null) return@LeScanCallback
            var isExist: Boolean = false
            for (item in deviceList) {
                if (item.name == bluetoothDevice.name) {
                    item.rssi = rssi
                    item.bluetoothDevice = bluetoothDevice
                    isExist = true
                    break;
                }
            }
            if (!isExist) {
                deviceList.add(bleDevice(bluetoothDevice.name, rssi, bluetoothDevice))
            }
            scanCallback(bluetoothDevice.name, rssi)
        }
    var scanCallback: (name: String, rssi: Int) -> Unit = { _, _ -> }
    var scanFlag: Boolean = false

    class bleDevice(var name: String, var rssi: Int, var bluetoothDevice: BluetoothDevice)

    var deviceList: MutableList<bleDevice> = ArrayList()
    var bluetoothGatt: BluetoothGatt? = null
    var bluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        @SuppressLint("StaticFieldLeak")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
//            Log.e("onConnectionStateChange", "status=" + status + "|" + "newState=" + newState);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                connectCallback(false, status)
                connectCallback = { _, _ -> }
                connectionStateChangeCallback(false)
                connectionStateChangeCallback = { _ -> }
                return
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                stopBluetoothDevicesDiscovery()
                connectCallback(true, 0)
                connectCallback = { _, _ -> }
                return
            }
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                bluetoothGatt?.close()
                connectCallback(false, 0)
                connectCallback = { _, _ -> }
                connectionStateChangeCallback(false)
                connectionStateChangeCallback = { _ -> }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            bluetoothGatt = gatt
            val bluetoothGattServices = gatt?.services
            val servicesList: MutableList<String> = ArrayList()
            if (bluetoothGattServices == null) getServicesCallback(servicesList)
            else {
                for (item in bluetoothGattServices) {
//                    Log.e("ble-service", "UUID=:" + item.uuid.toString())
                    servicesList.add(item.uuid.toString())
                }
                getServicesCallback(servicesList)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            val bytes = characteristic?.value
            if (bytes != null) {
//                Log.e("ble-receive", "读取成功[hex]:" + bytesToHexString(bytes));
//                Log.e("ble-receive", "读取成功[string]:" + String(bytes));
                characteristicChangedCallback(bytesToHexString(bytes), String(bytes))
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
//            if (BluetoothGatt.GATT_SUCCESS == status) {
//                Log.e("BleService", "onMtuChanged success MTU = " + mtu)
//            } else {
//                Log.e("BleService", "onMtuChanged fail ");
//            }
        }
    }
    var connectCallback: (ok: Boolean, errCode: Int) -> Unit = { _, _ -> }
    var reconnectTime = 0
    var connectionStateChangeCallback: (ok: Boolean) -> Unit = { _ -> }
    var getServicesCallback: (servicesList: List<String>) -> Unit = { _ -> }
    var characteristicChangedCallback: (hex: String, string: String) -> Unit = { _, _ -> }
//    val ecServerId = "0000FFE0-0000-1000-8000-00805F9B34FB"
    val ecServerId = "0000FFF0-0000-1000-8000-00805F9B34FB"
//    val ecWriteCharacteristicId = "0000FFE1-0000-1000-8000-00805F9B34FB"
    val ecWriteCharacteristicId = "0000FFF2-0000-1000-8000-00805F9B34FB"
    val ecReadCharacteristicId = "0000FFF1-0000-1000-8000-00805F9B34FB"

    @SuppressLint("StaticFieldLeak")
    private fun isLocServiceEnable(context: Context): Boolean {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gps: Boolean = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val network: Boolean =
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return gps || network
    }

    fun bluetoothAdapterInit(context: Context, activity: Activity): Int {
        bleContext = context
        bleActivity = activity
        if (bluetoothAdapter == null) bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothGatt?.close()
        if (bluetoothAdapter == null) {
            //设备不支持蓝牙
            return 1
        }
        if (!isLocServiceEnable(context)) {
            //定位开关没有开
            return 2
        }
        if (!getBluetoothAdapterState()) {
            openBluetoothAdapter()
            return 3
        }
        return 0
    }

    @SuppressLint("StaticFieldLeak")
    private fun openBluetoothAdapter() {
        bluetoothAdapter?.enable()
    }

    @SuppressLint("StaticFieldLeak")
    fun closeBluetoothAdapter() {
        bluetoothAdapter?.disable()
    }

    @SuppressLint("StaticFieldLeak")
    private fun getBluetoothAdapterState(): Boolean {
        return bluetoothAdapter?.isEnabled ?: false
    }

    @SuppressLint("StaticFieldLeak")
    fun startBluetoothDevicesDiscovery(callback: (name: String, rssi: Int) -> Unit) {
        scanCallback = callback
        if (!scanFlag) {
            bluetoothAdapter?.startLeScan(leScanCallback)
            scanFlag = true
        }
    }

    @SuppressLint("StaticFieldLeak")
    fun stopBluetoothDevicesDiscovery() {
        if (scanFlag) {
            bluetoothAdapter?.stopLeScan(leScanCallback)
            scanFlag = false
        }
    }

    @SuppressLint("StaticFieldLeak")
    private fun createBLEConnection(name: String, callback: (ok: Boolean, errCode: Int) -> Unit) {
        connectCallback = callback
        connectionStateChangeCallback = { _ -> }
        var isExist: Boolean = false
        for (item in deviceList) {
            if (item.name == name) {
                bluetoothGatt =
                    item.bluetoothDevice.connectGatt(bleContext, false, bluetoothGattCallback);
                isExist = true
                break;
            }
        }
        if (!isExist) {
            connectCallback(false, -1)
        }
    }

    @SuppressLint("StaticFieldLeak")
    fun closeBLEConnection() {
        bluetoothGatt?.disconnect()
    }

    @SuppressLint("StaticFieldLeak")
    private fun getBLEDeviceServices(callback: (servicesList: List<String>) -> Unit) {
        getServicesCallback = callback
        bluetoothGatt?.discoverServices();
    }

    //    ECBLE.getBLEDeviceCharacteristics("0000fff0-0000-1000-8000-00805f9b34fb")
    @SuppressLint("StaticFieldLeak")
    private fun getBLEDeviceCharacteristics(serviceId: String): MutableList<String> {
        val service = bluetoothGatt?.getService(UUID.fromString(serviceId))
        val listGattCharacteristic = service?.getCharacteristics()
        val characteristicsList: MutableList<String> = ArrayList()
        if (listGattCharacteristic == null) return characteristicsList
        for (item in listGattCharacteristic) {
//            Log.e("ble-characteristic", "UUID=:" + item.uuid.toString())
            characteristicsList.add(item.uuid.toString())
        }
        return characteristicsList
    }

    @SuppressLint("StaticFieldLeak")
    private fun notifyBLECharacteristicValueChange(
        serviceId: String,
        characteristicId: String
    ): Boolean {
        val service = bluetoothGatt?.getService(UUID.fromString(serviceId)) ?: return false
        val characteristicRead = service.getCharacteristic(UUID.fromString(characteristicId));
        val res =
            bluetoothGatt?.setCharacteristicNotification(characteristicRead, true) ?: return false
        if (!res) return false
        for (dp in characteristicRead.descriptors) {
            dp.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            bluetoothGatt?.writeDescriptor(dp)
        }
        return true
    }

    @SuppressLint("StaticFieldLeak")
    fun onBLECharacteristicValueChange(callback: (hex: String, string: String) -> Unit) {
        characteristicChangedCallback = callback
    }

    @SuppressLint("StaticFieldLeak")
    fun easyOneConnect(name: String, callback: (ok: Boolean) -> Unit) {
        createBLEConnection(name) { ok: Boolean, errCode: Int ->
//            Log.e("Connection", "res:" + ok + "|" + errCode)
            if (ok) {
//                onBLECharacteristicValueChange { hex: String, string: String ->
//                    Log.e("hex", hex)
//                    Log.e("string", string)
//                }
                getBLEDeviceServices() {
//                    for (item in it) {
//                        Log.e("ble-service", "UUID=" + item)
//                    }
                    getBLEDeviceCharacteristics(ecServerId)
                    notifyBLECharacteristicValueChange(ecServerId, ecReadCharacteristicId)
                    callback(true)
                    Thread() {
                        Thread.sleep(300);
                        setMtu(500)
                    }.start()
                }
            } else {
                callback(false)
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    fun easyConnect(name: String, callback: (ok: Boolean) -> Unit) {
        easyOneConnect(name) {
            if (it) {
                reconnectTime = 0
                callback(true)
            } else {
                reconnectTime = reconnectTime + 1
                if(reconnectTime>4){
                    reconnectTime = 0
                    callback(false)
                }
                else{
                    thread(start = true) {
                        easyConnect(name,callback)
                    }
                }
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    fun onBLEConnectionStateChange(callback: (ok: Boolean) -> Unit) {
        connectionStateChangeCallback = callback
    }

    @SuppressLint("StaticFieldLeak")
    fun offBLEConnectionStateChange() {
        connectionStateChangeCallback = { _ -> }
    }


    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("StaticFieldLeak")
    private fun writeBLECharacteristicValue(
        serviceId: String,
        characteristicId: String,
        data: String,
        isHex: Boolean
    ) {
        val byteArray: ByteArray? = if (isHex) toByteArray(data)
        else data.toByteArray()

        val service = bluetoothGatt?.getService(UUID.fromString(serviceId))
        val characteristicWrite = service?.getCharacteristic(UUID.fromString(characteristicId));

        characteristicWrite?.value = byteArray
        //设置回复形式
        characteristicWrite?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        //开始写数据
        bluetoothGatt
            ?.writeCharacteristic(characteristicWrite)
    }

    @SuppressLint("StaticFieldLeak")
    fun easySendData(data: String, isHex: Boolean) {
        writeBLECharacteristicValue("0000FFE0-0000-1000-8000-00805F9B34FB", "0000FFE1-0000-1000-8000-00805F9B34FB", data, isHex)
    }

    @SuppressLint("StaticFieldLeak")
    fun setMtu(v: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bluetoothGatt?.requestMtu(v)
        }
    }

    @SuppressLint("StaticFieldLeak")
    fun bytesToHexString(bytes: ByteArray?): String {
        if (bytes == null) return ""
        var str = ""
        for (b in bytes) {
            str += String.format("%02X", b)
        }
        return str
    }

    @SuppressLint("StaticFieldLeak")
    private fun toByteArray(hexString: String): ByteArray? {
        val byteArray = ByteArray(hexString.length / 2)
        var k = 0
        for (i in byteArray.indices) {
            val high =
                (Character.digit(hexString[k], 16) and 0xf).toByte()
            val low =
                (Character.digit(hexString[k + 1], 16) and 0xf).toByte()
            byteArray[i] = (high * 16 + low).toByte()
            k += 2
        }
        return byteArray
    }
}
