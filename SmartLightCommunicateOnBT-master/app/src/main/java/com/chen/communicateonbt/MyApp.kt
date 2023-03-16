package com.chen.communicateonbt

import android.app.Application
import com.kongzue.dialogx.DialogX
import com.kongzue.dialogx.style.MaterialStyle

class MyApp: Application() {
    override fun onCreate() {
        super.onCreate()
        DialogX.init(this)
        DialogX.globalStyle = MaterialStyle.style()
    }
}