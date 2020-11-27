package com.junianto.facemaskdetection.ui.camera.recoginition

import androidx.lifecycle.ViewModel
import com.junianto.facemaskdetection.injection.annotation.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class RecognitionModule {
    @Binds
    @IntoMap
    @ViewModelKey(RecognitionViewModel::class)
    abstract fun bindViewModel(recognitionViewModel: RecognitionViewModel): ViewModel
}