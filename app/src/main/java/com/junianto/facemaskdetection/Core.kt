package com.junianto.facemaskdetection

import com.junianto.facemaskdetection.injection.DaggerAppComponent
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication

class Core : DaggerApplication() {
    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerAppComponent.builder().create(this).build()
    }
}