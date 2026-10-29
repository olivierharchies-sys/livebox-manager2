package com.example.liveboxmanager.data

/** Représente un appareil connecté à la Livebox. */
data class Device(
    val mac: String,
    val name: String,
    val ipAddress: String? = null,
    val vendor: String? = null,
    val connectionType: String? = null, // wifi / ethernet / plc
    val active: Boolean = true,
    val wifiBand: String? = null,
)

/** État du WiFi principal. */
data class WifiConfig(
    val ssid24: String = "",
    val ssid5: String = "",
    val channel24: Int = 6,
    val channel5: Int = 36,
    val enabled24: Boolean = true,
    val enabled5: Boolean = true,
    val securityMode: String = "WPA2-PSK",
    val hidingEnabled: Boolean = false,
)

data class ParentalRule(
    val id: String,
    val deviceId: String,
    val deviceName: String,
    val schedule: String = "22:00 - 07:00",
    val enabled: Boolean = true,
)

enum class FirewallLevel(val label: String) {
    OFF("Désactivé (déconseillé)"),
    MEDIUM("Moyen — blocage entrant"),
    HIGH("Élevé — blocage entrant/sortant suspect");
    override fun toString(): String = label
}

data class FirewallStatus(
    val level: FirewallLevel = FirewallLevel.MEDIUM,
    val upnpEnabled: Boolean = true,
    val pingBlocked: Boolean = true,
    val ipv6FirewallEnabled: Boolean = true,
)

data class LiveboxInfo(
    val model: String = "Livebox",
    val firmwareVersion: String = "",
    val uptimeSeconds: Long = 0,
    val wanStatus: String = "inconnu",
)
