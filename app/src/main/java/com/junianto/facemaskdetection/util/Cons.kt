package com.junianto.facemaskdetection.util

import android.Manifest

class Cons {
    companion object {
        const val MAX_RESULT_DISPLAY = 3
        const val TAG = "Face Mask Detection"
        const val REQUEST_CODE_PERMISSIONS = 23
        val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}