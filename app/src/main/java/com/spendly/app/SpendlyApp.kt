package com.spendly.app

import android.app.Application
import com.spendly.app.core.di.SpendlyContainer

class SpendlyApp : Application() {
    lateinit var container: SpendlyContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = SpendlyContainer(this)
    }
}
