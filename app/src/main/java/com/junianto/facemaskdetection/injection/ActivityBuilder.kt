package com.junianto.facemaskdetection.injection

import com.junianto.facemaskdetection.ui.camera.CameraActivity
import com.junianto.facemaskdetection.ui.camera.recoginition.RecognitionModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ActivityBuilder {

    @ContributesAndroidInjector(modules = [RecognitionModule::class])
    abstract fun bindCameraActivity(): CameraActivity
}