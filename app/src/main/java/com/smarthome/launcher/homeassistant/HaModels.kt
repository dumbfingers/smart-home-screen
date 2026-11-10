package com.smarthome.launcher.homeassistant

enum class EntityDomain {
    LIGHT,
    SWITCH,
    CLIMATE,
    SENSOR,
    MEDIA_PLAYER,
    CAMERA,
    OTHER
}

data class HaEntity(
    val entityId: String,
    val state: String,
    val friendlyName: String,
    val domain: EntityDomain,
    val unitOfMeasurement: String? = null,
    val icon: String? = null,
    val brightness: Int? = null,
    val currentTemperature: Double? = null,
    val targetTemperature: Double? = null
) {
    val isTurnedOn: Boolean
        get() = state.equals("on", ignoreCase = true)
}
