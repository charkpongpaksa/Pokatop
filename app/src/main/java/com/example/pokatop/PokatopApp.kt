package com.example.pokatop

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.pokatop.data.Prefs

class PokatopApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(Prefs(this).nightMode)
    }
}
