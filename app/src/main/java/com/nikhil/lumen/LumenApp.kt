package com.nikhil.lumen

import android.app.Application
import android.content.Context
import com.nikhil.lumen.data.AppRepository
import com.nikhil.lumen.data.ImageLoader
import com.nikhil.lumen.data.InputsRepository
import com.nikhil.lumen.data.SettingsStore
import com.nikhil.lumen.data.WatchNextRepository
import com.nikhil.lumen.system.HomeSetup

class LumenApp : Application() {
    lateinit var settings: SettingsStore
        private set
    val apps by lazy { AppRepository(this) }
    val inputs by lazy { InputsRepository(this) }
    val watchNext by lazy { WatchNextRepository(this) }
    val images by lazy { ImageLoader(this) }

    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore(this)

        // Record crashes so the next start can open recovery mode instead of looping.
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { settings.recordCrash(System.currentTimeMillis()) }
            previous?.uncaughtException(thread, error)
        }

        // Strong mode: if the user granted WRITE_SECURE_SETTINGS, keep the Home override on
        // after force stops and updates.
        if (!HomeSetup.isDefaultHome(this)) HomeSetup.enableOverride(this)
    }

    companion object {
        fun from(context: Context): LumenApp = context.applicationContext as LumenApp
    }
}
