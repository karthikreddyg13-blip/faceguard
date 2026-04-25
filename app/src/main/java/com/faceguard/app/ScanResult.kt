package com.faceguard.app

sealed class ScanResult {
    object Owner : ScanResult()
    data class KnownPerson(val profile: Profile) : ScanResult()
    object Stranger : ScanResult()
    data class Failed(val reason: String) : ScanResult()
}