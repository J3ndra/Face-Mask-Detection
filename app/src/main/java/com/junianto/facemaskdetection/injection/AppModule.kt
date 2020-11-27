package com.junianto.facemaskdetection.injection

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.junianto.facemaskdetection.base.ViewModelFactory
import dagger.Binds
import dagger.Module

@Module
abstract class AppModule {

    @Binds
    abstract fun provideContext(application: Application): Context

    @Binds
    abstract fun bindViewModelFactory(viewModelFactory: ViewModelFactory): ViewModelProvider.Factory
}