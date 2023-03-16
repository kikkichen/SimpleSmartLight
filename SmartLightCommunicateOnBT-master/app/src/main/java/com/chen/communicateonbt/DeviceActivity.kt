package com.chen.communicateonbt

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.chen.communicateonbt.databinding.ActivityDevicesBinding
import com.chen.communicateonbt.viewmodel.DeviceControllerViewModel
import com.chen.communicateonbt.viewmodel.LightType
import com.chen.communicateonbt.viewmodel.PatternType
import com.chen.communicateonbt.viewmodel.TimerLevel
import com.kongzue.dialogx.dialogs.MessageDialog
import com.kongzue.dialogx.interfaces.OnBindView
import com.kongzue.dialogx.style.MaterialStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class DeviceActivity : AppCompatActivity() {

    val viewModel : DeviceControllerViewModel by viewModels()

    private val binding: ActivityDevicesBinding by lazy {
        ActivityDevicesBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initView()

        checkBlePermission()
        binding.ivBack.setOnClickListener {
            ECBLE.offBLEConnectionStateChange()
            ECBLE.closeBLEConnection()
            finish()
        }

        ECBLE.onBLEConnectionStateChange {
            showToast("设备断开")
        }
    }

    private fun initView() {
        viewModel.boot_sign.observe(this) {
            if (it) {
                binding.switchLightType.isEnabled = true
                binding.switchPattern.isEnabled = true
                binding.switchTimer.isEnabled = true
            } else {
                binding.switchLightType.isEnabled = false
                binding.switchPattern.isEnabled = false
                binding.switchTimer.isEnabled = false
            }
        }

        // 开机开关
        binding.switchOpen.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.boot_sign.value = true
                ECBLE.easySendData("01", true)
                ECBLE.easySendData(viewModel.TranslateColorData(LightType.WHITE), true)
            } else {
                viewModel.isLightTypeUsable.value = false
                binding.switchLightType.isChecked = false
                viewModel.isPatternTypeUsable.value = false
                binding.switchPattern.isChecked = false
                viewModel.isTimerUsable.value = false
                binding.switchTimer.isChecked = false
                viewModel.boot_sign.value = false
                ECBLE.easySendData("00", true)
            }
        }
        // 氛围灯可用
        binding.switchLightType.setOnCheckedChangeListener { _ , isCheck ->
            viewModel.isLightTypeUsable.value = isCheck
            if (!isCheck) {
                ECBLE.easySendData("07", true)
            }
        }

        // 图案点阵启用
        binding.switchPattern.setOnCheckedChangeListener { _, isCheck ->
            viewModel.isPatternTypeUsable.value = isCheck
            if (isCheck) {
                ECBLE.easySendData("14", true)
            } else {
                ECBLE.easySendData("15", true)
            }
        }

        // 定时关闭启用
        binding.switchTimer.setOnCheckedChangeListener { _, isCheck ->
            viewModel.isTimerUsable.value = isCheck
            if (!isCheck) {
                ECBLE.easySendData(viewModel.TranslateTimer(TimerLevel.NONE), true)
            }
        }

        // 氛围灯选择菜单启用
        viewModel.isLightTypeUsable.observe(this) { isUsable ->
            binding.buttonLightType.isEnabled = isUsable
        }

        // 图形点正变更菜单可用
        viewModel.isPatternTypeUsable.observe(this) { isUsable ->
            binding.buttonPattern.isEnabled = isUsable
        }

        // 定时菜单可用
        viewModel.isTimerUsable.observe(this) { isUsable ->
            binding.buttonTimer.isEnabled = isUsable
        }

        binding.buttonLightType.setOnClickListener {
            MessageDialog.build()
                .setStyle(MaterialStyle.style())
                .setTitle("选择氛围灯")
                .setOkButton("关闭")
                .setCustomView(object : OnBindView<MessageDialog>(R.layout.select_light_type_layout) {
                    override fun onBind(dialog: MessageDialog?, v: View?) {
                        v?.findViewById<RadioButton>(R.id.white_light)?.setOnClickListener() {
                            ECBLE.easySendData(viewModel.TranslateColorData(LightType.WHITE), true)
                        }
                        v?.findViewById<RadioButton>(R.id.blue_light)?.setOnClickListener() {
                            ECBLE.easySendData(viewModel.TranslateColorData(LightType.BLUE), true)
                        }
                        v?.findViewById<RadioButton>(R.id.red_light)?.setOnClickListener() {
                            ECBLE.easySendData(viewModel.TranslateColorData(LightType.RED), true)
                        }
                        v?.findViewById<RadioButton>(R.id.green_light)?.setOnClickListener() {
                            ECBLE.easySendData(viewModel.TranslateColorData(LightType.GREEN), true)
                        }
                        v?.findViewById<RadioButton>(R.id.yellow_light)?.setOnClickListener() {
                            ECBLE.easySendData(viewModel.TranslateColorData(LightType.YELLOW), true)
                        }
                        v?.findViewById<RadioButton>(R.id.colorful_light)?.setOnClickListener() {
                            ECBLE.easySendData(viewModel.TranslateColorData(LightType.COLORFUL), true)
                        }
                    }
                })
                .show()
        }

        binding.buttonPattern.setOnClickListener {
            MessageDialog.build()
                .setStyle(MaterialStyle.style())
                .setTitle("选择点阵图形")
                .setOkButton("关闭")
                .setCustomView(object : OnBindView<MessageDialog>(R.layout.select_pattern_type_layout) {
                    override fun onBind(dialog: MessageDialog?, v: View?) {
                        v?.findViewById<RadioButton>(R.id.pattern_monster)?.setOnClickListener() {
                            ECBLE.easySendData(viewModel.TranslatePattern(PatternType.MONSTER), true)
                        }
                        v?.findViewById<RadioButton>(R.id.pattern_square)?.setOnClickListener() {
                            ECBLE.easySendData(viewModel.TranslatePattern(PatternType.SQUARE), true)
                        }
                        v?.findViewById<RadioButton>(R.id.pattern_heart)?.setOnClickListener() {
                            ECBLE.easySendData(viewModel.TranslatePattern(PatternType.HEART), true)
                        }
                        v?.findViewById<RadioButton>(R.id.pattern_ghost)?.setOnClickListener() {
                            ECBLE.easySendData(viewModel.TranslatePattern(PatternType.GHOST), true)
                        }
                        v?.findViewById<RadioButton>(R.id.pattern_diamond)?.setOnClickListener() {
                            ECBLE.easySendData(viewModel.TranslatePattern(PatternType.DIAMOND), true)
                        }
                        v?.findViewById<RadioButton>(R.id.pattern_star)?.setOnClickListener() {
                            ECBLE.easySendData(viewModel.TranslatePattern(PatternType.STAR), true)
                        }
                    }

                }).show()
        }

        binding.buttonTimer.setOnClickListener {
            MessageDialog.build()
                .setStyle(MaterialStyle.style())
                .setTitle("定时关闭")
                .setOkButton("关闭")
                .setCustomView(object  : OnBindView<MessageDialog>(R.layout.select_timer_layout) {
                    override fun onBind(dialog: MessageDialog?, v: View?) {
                        v?.findViewById<RadioButton>(R.id.set_time_15s)?.setOnClickListener() {
                            ECBLE.easySendData(viewModel.TranslateTimer(TimerLevel.SHORT), true)
                        }
                        v?.findViewById<RadioButton>(R.id.set_time_30s)?.setOnClickListener() {
                            ECBLE.easySendData(viewModel.TranslateTimer(TimerLevel.MIDDLE), true)
                        }
                        v?.findViewById<RadioButton>(R.id.set_time_1m05s)?.setOnClickListener() {
                            ECBLE.easySendData(viewModel.TranslateTimer(TimerLevel.LONG), true)
                        }
                        v?.findViewById<RadioButton>(R.id.set_time_1m30s)?.setOnClickListener() {
                            ECBLE.easySendData(viewModel.TranslateTimer(TimerLevel.SUPPER), true)
                        }
                    }
                }).show()
        }
    }

    override fun onStop() {
        super.onStop()
    }

    fun showToast(text: String) {
        runOnUiThread {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        }
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

    fun checkBlePermission() {
        if (ActivityCompat.checkSelfPermission(ECBLE.bleContext!!, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)
        } else {
            Log.i("_chen", "已申请权限")
        }
    }
}