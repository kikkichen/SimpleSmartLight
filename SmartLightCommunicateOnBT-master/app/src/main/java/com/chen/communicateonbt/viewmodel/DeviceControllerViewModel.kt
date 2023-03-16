package com.chen.communicateonbt.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.sql.Time

// 氛围灯颜色种类
enum class LightType {
    BLUE, RED, GREEN, YELLOW, WHITE, COLORFUL
}

enum class PatternType {
    MONSTER, SQUARE, HEART, GHOST, DIAMOND, STAR
}

enum class TimerLevel {
    NONE,SHORT, MIDDLE, LONG, SUPPER
}

class DeviceControllerViewModel: ViewModel() {

    // boot
    var boot_sign : MutableLiveData<Boolean> = MutableLiveData(false)

    // usable for button
    val isLightTypeUsable : MutableLiveData<Boolean> = MutableLiveData(false)
    val isPatternTypeUsable : MutableLiveData<Boolean> = MutableLiveData(false)
    val isTimerUsable : MutableLiveData<Boolean> = MutableLiveData(false)

    fun TranslateColorData(color : LightType) : String {
        return when(color) {
            LightType.BLUE -> "03"
            LightType.RED -> "04"
            LightType.GREEN -> "05"
            LightType.YELLOW -> "06"
            LightType.WHITE -> "07"
            LightType.COLORFUL -> "02"
        }
    }

    fun TranslatePattern(pattern: PatternType) : String {
        return when(pattern) {
            PatternType.MONSTER -> "08"
            PatternType.SQUARE -> "09"
            PatternType.HEART -> "10"
            PatternType.GHOST -> "11"
            PatternType.DIAMOND -> "12"
            PatternType.STAR -> "13"
        }
    }

    fun TranslateTimer(timer: TimerLevel) : String {
        return when(timer) {
            TimerLevel.NONE -> "90"
            TimerLevel.SHORT -> "91"
            TimerLevel.MIDDLE -> "92"
            TimerLevel.LONG -> "93"
            TimerLevel.SUPPER -> "94"
        }
    }

}