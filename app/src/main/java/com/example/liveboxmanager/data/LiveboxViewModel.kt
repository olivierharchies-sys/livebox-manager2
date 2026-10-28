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
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                api = LiveboxApi(host)
                api.login(password)
            }
            isLoggedIn = ok
            isLoading = false
            message = if (ok) null else "Échec de connexion — vérifiez le mot de passe admin."
            if (ok) refreshAll()
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
                api.setWifiSsid("wl0", config.ssid24)
                api.setWifiSsid("wl1", config.ssid5)
                api.setWifiChannel("wl0", config.channel24)
                api.setWifiChannel("wl1", config.channel5)
                wifi = api.fetchWifiConfig()
            }
            message = "Configuration WiFi appliquée."
        }
    }

    fun updateFirewall(status: FirewallStatus) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                api.setFirewallLevel(status.level)
                api.setUpnp(status.upnpEnabled)
                firewall = api.fetchFirewall()
            }
            message = "Pare-feu mis à jour."
        }
    }

    fun toggleParental(rule: ParentalRule) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                api.setDeviceSchedule(rule.deviceId, !rule.enabled, rule.schedule)
            }
            parentalRules = parentalRules.map {
                if (it.id == rule.id) it.copy(enabled = !it.enabled) else it
            }
        }
    }
}
