package es.colefinder.data.model

data class AppUpdateConfig(
    val minVersionCode: Int,
    val updateType: String, // "FLEXIBLE" o "IMMEDIATE"
)
