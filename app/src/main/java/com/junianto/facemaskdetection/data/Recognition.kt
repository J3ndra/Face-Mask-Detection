package com.junianto.facemaskdetection.data

data class Recognition (
    val label: String,
    val confidence: Float
) {
    override fun toString(): String {
        return "$label / $probabilityString"
    }

    // Output probability as a string to enable easy data binding
    val probabilityString = if (confidence < 0.2f || confidence > 0.8f) String.format("%.1f%%", confidence * 100.0f) else "Low"
}