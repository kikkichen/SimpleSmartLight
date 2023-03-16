package com.chen.communicateonbt

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() ,EasyPermissions.PermissionCallbacks{

    var deviceListData: MutableList<DeviceInfo> = ArrayList()
    var listView: ListView? = null
    var listViewAdapter: Adapter? = null
    var connectDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        uiInit()

        //申请权限
        permissionsInit()
    }

    override fun onResume() {
        super.onResume()
        deviceListData.clear()
        listViewAdapter?.notifyDataSetChanged()
        bluetoothInit()
    }

    override fun onStop() {
        super.onStop()
        //停止扫描，给手机省电
        ECBLE.stopBluetoothDevicesDiscovery()
    }

    class DeviceInfo(var name: String, var rssi: Int)

    class Adapter(context: Context, val resourceId: Int, objects: List<DeviceInfo>) :
        ArrayAdapter<DeviceInfo>(context, resourceId, objects) {
        @SuppressLint("ViewHolder")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val deviceInfo: DeviceInfo? = getItem(position) //获取当前项的实例
            val name = deviceInfo?.name ?: ""
            val rssi = deviceInfo?.rssi ?: 0
            val view: View = LayoutInflater.from(context).inflate(resourceId, parent, false)
            val headImg = view.findViewById<ImageView>(R.id.iv_type)
            if (name.first() == '@' && name.length == 11) {
                headImg.setImageResource(R.drawable.ecble)
            } else {
                headImg.setImageResource(R.drawable.ble)
            }
            view.findViewById<TextView>(R.id.tv_name).text = name
            view.findViewById<TextView>(R.id.tv_rssi).text = "" + rssi
            val rssiImg = view.findViewById<ImageView>(R.id.iv_rssi)
            when {
                rssi >= -41 -> rssiImg.setImageResource(R.drawable.s5)
                rssi >= -55 -> rssiImg.setImageResource(R.drawable.s4)
                rssi >= -65 -> rssiImg.setImageResource(R.drawable.s3)
                rssi >= -75 -> rssiImg.setImageResource(R.drawable.s2)
                rssi < -75 -> rssiImg.setImageResource(R.drawable.s1)
            }

            return view
        }
    }

    fun uiInit() {
        //下拉刷新
        val swipRefreshLayout: SwipeRefreshLayout = findViewById(R.id.swipe_layout)
        swipRefreshLayout.setColorSchemeColors(0x01a4ef)
        swipRefreshLayout.setOnRefreshListener {
            //清空数据
            deviceListData.clear()
            listViewAdapter?.notifyDataSetChanged()
            ECBLE.stopBluetoothDevicesDiscovery()
            Handler().postDelayed({
                swipRefreshLayout.isRefreshing = false
                bluetoothInit()
            }, 1000)
        }

        //列表初始化
        listView = findViewById<ListView>(R.id.list_view)
        listViewAdapter = Adapter(this, R.layout.list_item, deviceListData)
        listView?.adapter = listViewAdapter
        listView?.setOnItemClickListener { adapterView: AdapterView<*>, view1: View, i: Int, l: Long ->
            showConnectDialog()
            ECBLE.easyConnect(deviceListData.get(i).name) {
                hideConnectDialog()
                if (it) {
//                    showToast("连接成功")
                    //跳转设备页
                    runOnUiThread {
                        startActivity(Intent().setClass(this, DeviceActivity().javaClass))
                    }
                } else {
                    showToast("连接失败")
                }
            }
        }
        listRefresh();
    }

    fun listRefresh() {
        Handler().postDelayed({
            listViewAdapter?.notifyDataSetChanged()
            listRefresh()
        }, 500)
    }

    fun showAlert(title: String, content: String, callback: () -> Unit) {
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton("确定",
                    DialogInterface.OnClickListener { _, _ -> callback() })
                .setCancelable(false)
                .create().show()
        }
    }

    fun showConnectDialog() {
        runOnUiThread {
            if (connectDialog == null) {
                connectDialog = ProgressDialog(this)
                connectDialog?.setMessage("正在连接中...")
            }
            connectDialog?.show()
        }
    }

    fun hideConnectDialog() {
        runOnUiThread {
            connectDialog?.dismiss()
        }
    }

    fun showToast(text: String) {
        runOnUiThread {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        }
    }

    fun bluetoothInit() {
        val res = ECBLE.bluetoothAdapterInit(this, this)
        if (res == 1) {
            showAlert("提示", "设备不支持蓝牙，软件无法使用") {
                exitProcess(0)//退出app
            }
            return
        }
        if (res == 2) {
            showAlert("提示", "请先打开系统定位开关，再重新启动应用") {
                exitProcess(0)//退出app
            }
            return
        }
        if (res == 3) {
            showAlert("提示", "请先打开系统蓝牙开关，再重新启动应用") {
                exitProcess(0)//退出app
            }
            return
        }

        ECBLE.startBluetoothDevicesDiscovery { name, rssi ->
            runOnUiThread {
//                Log.e("Discovery", name + "|" + rssi)
                var isExist: Boolean = false
                for (item in deviceListData) {
                    if (item.name == name) {
                        item.rssi = rssi
                        isExist = true
                        break;
                    }
                }
                if (!isExist) {
                    deviceListData.add(MainActivity.DeviceInfo(name, rssi))
                }
            }
        }
    }

    //申请权限
    fun permissionsInit() {
        //申请位置权限
        val perms = arrayOf<String>(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (EasyPermissions.hasPermissions(this, *perms)) {
            // Already have permission, do the thing
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(
                this, "请先授权应用定位权限",
                0, *perms
            );
        }
    }

    //权限回调
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    //权限获取失败
    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        //跳转到权限设置界面
        AppSettingsDialog.Builder(this)
            .setTitle("提示")
            .setRationale("请先打开应用定位权限，再重新启动应用")
            .build().show()
    }

    //权限获取成功
    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
    }

    @AfterPermissionGranted(0)
    private fun requestPermissions() {
        //所有权限都获取成功了
        showAlert("提示", "获取权限成功，请重新启动应用") {
            exitProcess(0)//退出app
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            // Do something after user returned from app settings screen, like showing a Toast.
            //用户从权限设置界面跳转到应用，退出app
            showAlert("提示", "请重新启动应用") {
                exitProcess(0)//退出app
            }
        }
    }
}