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
 * ViewModel partagé : conserve la session Livebox et expose
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

    fun login(host: String, password: String) {
        isLoading = true
        message = null
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    api = LiveboxApi(host)
                    api.login(password)
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
                    "Erreur de connexion : " + (err.message ?: "impossible de joindre la Livebox. Vérifiez que vous êtes bien sur son Wi-Fi.")
                } else {
                    "Échec de connexion — vérifiez le mot de passe admin."
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
            withContext(Dispatchers.IO) {
                runCatching { api.setWifiSsid("wl0", config.ssid24) }
                runCatching { api.setWifiSsid("wl1", config.ssid5) }
                runCatching { api.setWifiChannel("wl0", config.channel24) }
                runCatching { api.setWifiChannel("wl1", config.channel5) }
                runCatching { wifi = api.fetchWifiConfig() }
            }
            message = "Configuration WiFi appliquée."
        }
    }

    fun updateFirewall(status: FirewallStatus) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { api.setFirewallLevel(status.level) }
                runCatching { api.setUpnp(status.upnpEnabled) }
                runCatching { firewall = api.fetchFirewall() }
            }
            message = "Pare-feu mis à jour."
        }
    }

    fun toggleParental(rule: ParentalRule) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { api.setDeviceSchedule(rule.deviceId, !rule.enabled, rule.schedule) }
            }
            parentalRules = parentalRules.map {
                if (it.id == rule.id) it.copy(enabled = !rule.enabled) else it
            }
        }
    }
}
