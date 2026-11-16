package com.example.pokatop.sensing

object Detectors {
    fun create(): PostureDetector = FilteredFlipDetector()
}
