package com.junianto.facemaskdetection.ui.camera.recoginition

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.junianto.facemaskdetection.base.BaseViewModel
import com.junianto.facemaskdetection.data.Recognition
import javax.inject.Inject

class RecognitionViewModel @Inject constructor() : BaseViewModel() {

    /**
     * This is a LiveData field. Choosing this structure because the whole list tend to be updated
     * at once in ML and not individual elements. Updating this once for the entire list makes
     * sense.
     */
    private val _recognitionList = MutableLiveData<List<Recognition>>()
    val recognitionList: LiveData<List<Recognition>> = _recognitionList

    fun updateData(recognitions: List<Recognition>){
        _recognitionList.postValue(recognitions)
    }
}