package com.example.liveboxmanager.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel partagé : conserve la session du modem et expose
 * un état observable pour les écrans Compose.
 */
class LiveboxViewModel : ViewModel() {

    var api = LiveboxApi()
        private set

    var isLoggedIn by mutableStateOf(false)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    var info by mutableStateOf(LiveboxInfo())
        private set
    var devices by mutableStateOf<List<Device>>(emptyList())
        private set
    var wifi by mutableStateOf(WifiConfig())
        private set
    var firewall by mutableStateOf(FirewallStatus())
        private set
    var parentalRules by mutableStateOf(listOf<ParentalRule>())
        private set

    fun login(host: String, username: String, password: String) {
        isLoading = true
        message = null
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    api = LiveboxApi(host)
                    api.login(password, username)
                }
            }
            val ok = result.getOrDefault(false)
            isLoading = false
            if (ok) {
                isLoggedIn = true
                refreshAll()
            } else {
                val err = result.exceptionOrNull()
                message = if (err != null) {
                    "Erreur de connexion : " + (err.message ?: "impossible de joindre le modem. Vérifiez que vous êtes bien sur son Wi-Fi.")
                } else {
                    "Échec de connexion — vérifiez l'identifiant et le mot de passe."
                }
            }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            isLoading = true
            withContext(Dispatchers.IO) {
                runCatching { info = api.fetchInfo() }
                runCatching { devices = api.fetchDevices() }
                runCatching { wifi = api.fetchWifiConfig() }
                runCatching { firewall = api.fetchFirewall() }
                runCatching {
                    parentalRules = devices.map {
                        ParentalRule(it.mac, it.mac, it.name, enabled = false)
                    }
                }
            }
            isLoading = false
        }
    }

    fun updateWifi(config: WifiConfig) {
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                listOf(
                    runCatching { api.setWifiSsid("wl0", config.ssid24) }.getOrDefault(false),
                    runCatching { api.setWifiSsid("wl1", config.ssid5) }.getOrDefault(false),
                    runCatching { api.setWifiChannel("wl0", config.channel24) }.getOrDefault(false),
                    runCatching { api.setWifiChannel("wl1", config.channel5) }.getOrDefault(false),
                    runCatching { wifi = api.fetchWifiConfig() }.isSuccess,
                )
            }
            message = if (results.take(4).all { it }) {
                "Configuration WiFi appliquée."
            } else {
                "Lecture OK, mais ce firmware a refusé la modification du WiFi (essayez depuis l'interface web du modem)."
            }
        }
    }

    fun updateFirewall(status: FirewallStatus) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { api.setFirewallLevel(status.level) }.getOrDefault(false)
            }
            message = if (ok) "Pare-feu mis à jour." else "Le pare-feu n'est pas modifiable via cette API sur ce modem."
        }
    }

    fun toggleParental(rule: ParentalRule) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { api.setDeviceSchedule(rule.deviceId, !rule.enabled, rule.schedule) }.getOrDefault(false)
            }
            if (ok) {
                parentalRules = parentalRules.map {
                    if (it.id == rule.id) it.copy(enabled = !rule.enabled) else it
                }
            }
            message = if (ok) "Règle mise à jour." else "Le contrôle parental n'est pas exposé par l'API de ce modem."
        }
    }
}
