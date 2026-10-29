package com.example.liveboxmanager.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Client minimaliste de l'API locale "sysbus" de la Livebox (Orange).
 *
 * Principe (identique à l'interface web http://livebox ou http://192.168.1.1) :
 *  1. POST /ws avec service "sah.Device.Interface" / méthode "authenticate"
 *     en Authorization Basic (admin / mot de passe admin).
 *  2. La Livebox répond un contexte + un cookie "sah/context" à renvoyer
 *     sur les appels suivants.
 *  3. Chaque appel suivant est un POST /ws { service, method, parameters }.
 *
 * L'app doit être sur le même réseau Wi-Fi que la Livebox.
 */
class LiveboxApi(
    private var host: String = "192.168.1.1",
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json".toMediaType()
    private var contextCookie: String? = null

    val baseUrl: String get() = "http://" + host

    /** Authentifie l'utilisateur et stocke le cookie de session. */
    fun login(password: String, username: String = "admin"): Boolean {
        val auth = okhttp3.Credentials.basic(username, password)
        val body = ("{\"service\":\"sah.Device.Interface\",\"method\":\"authenticate\"," +
                "\"parameters\":{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}}")
            .toRequestBody(jsonMedia)
        val request = Request.Builder()
            .url(baseUrl + "/ws")
            .header("Authorization", auth)
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("L'appareil a répondu avec le code HTTP " + response.code + " — ce n'est probablement pas une Livebox.")
            }
            val text = response.body?.string() ?: throw IllegalStateException("Réponse vide de l'appareil.")
            if (text.trim().startsWith("<")) {
                throw IllegalStateException("L'appareil à " + host + " a répondu une page web classique : ce n'est probablement pas une Livebox Orange (interface incompatible).")
            }
            val json = runCatching { JSONObject(text) }.getOrElse {
                throw IllegalStateException("Réponse inattendue de l'appareil à " + host + " : ce n'est probablement pas une Livebox Orange.")
            }
            if (!json.optBoolean("status", false)) return false
            val headers = response.headers
            contextCookie = buildString {
                var first = true
                for ((name, value) in headers) {
                    if (name.equals("set-cookie", ignoreCase = true)) {
                        if (!first) append("; ")
                        append(value)
                        first = false
                    }
                }
            }.ifEmpty { null }
            return true
        }
    }

    /** Appel générique à l'API sysbus. */
    fun call(service: String, method: String, parameters: JSONObject = JSONObject()): JSONObject? {
        val payload = JSONObject()
            .put("service", service)
            .put("method", method)
            .put("parameters", parameters)
        val builder = Request.Builder()
            .url(baseUrl + "/ws")
            .post(payload.toString().toRequestBody(jsonMedia))
        contextCookie?.let { builder.header("Cookie", it) }
        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) return null
            val text = response.body?.string() ?: return null
            if (text.isEmpty()) return null
            return JSONObject(text).optJSONObject("result")
        }
    }

    // ---- Lecture ----

    fun fetchInfo(): LiveboxInfo {
        val res = call("DeviceInfo", "get") ?: return LiveboxInfo()
        return LiveboxInfo(
            model = res.optString("ProductClass", "Livebox"),
            firmwareVersion = res.optString("SoftwareVersion", ""),
            uptimeSeconds = res.optLong("UpTime", 0),
            wanStatus = if (res.optJSONObject("WAN")?.optString("Status") == "Up") "connecté" else "hors ligne",
        )
    }

    fun fetchDevices(): List<Device> {
        val res = call("NeMo.Intf.lan", "getMIBs", JSONObject().put("mibs", "wifi_neigh_ap")) ?: return emptyList()
        val devices = mutableListOf<Device>()
        val status = res.optJSONObject("status") ?: return emptyList()
        val neigh = status.optJSONObject("wifi_neigh_ap")?.optJSONObject("associatedclients") ?: return emptyList()
        for (mac in neigh.keys()) {
            val entry = neigh.getJSONObject(mac)
            devices.add(
                Device(
                    mac = mac,
                    name = entry.optString("FriendlyName", entry.optString("Name", mac)),
                    ipAddress = entry.optString("IPAddress").ifEmpty { null },
                    connectionType = entry.optString("MediaType").ifEmpty { null },
                    active = entry.optBoolean("Active", true),
                )
            )
        }
        return devices
    }

    fun fetchWifiConfig(): WifiConfig {
        val res = call("NeMo.Intf.wl0", "getMIBs") ?: return WifiConfig()
        val s = res.optJSONObject("status") ?: return WifiConfig()
        fun mib(iface: String): JSONObject = s.optJSONObject(iface) ?: JSONObject()
        val wl0 = mib("wl0")
        val wl1 = mib("wl1")
        return WifiConfig(
            ssid24 = wl0.optString("SSID"),
            channel24 = wl0.optJSONObject("channel")?.optInt("channel", 6) ?: 6,
            ssid5 = wl1.optString("SSID"),
            channel5 = wl1.optJSONObject("channel")?.optInt("channel", 36) ?: 36,
        )
    }

    fun fetchFirewall(): FirewallStatus {
        val res = call("Firewall", "get") ?: return FirewallStatus()
        return FirewallStatus(
            level = when (res.optString("Mode").lowercase()) {
                "off", "low" -> FirewallLevel.OFF
                "high" -> FirewallLevel.HIGH
                else -> FirewallLevel.MEDIUM
            },
            upnpEnabled = res.optBoolean("UPnPEnabled", true),
            pingBlocked = res.optBoolean("PingBlocked", true),
            ipv6FirewallEnabled = res.optBoolean("IPv6Enabled", true),
        )
    }

    // ---- Écriture ----

    fun setWifiSsid(interfaceName: String, ssid: String): Boolean =
        call("NeMo.Intf." + interfaceName, "set", JSONObject().put("SSID", ssid)) != null

    fun setWifiChannel(interfaceName: String, channel: Int): Boolean =
        call("NeMo.Intf." + interfaceName, "set", JSONObject().put("Channel", channel)) != null

    fun setWifiEnabled(interfaceName: String, enabled: Boolean): Boolean =
        call("NeMo.Intf." + interfaceName, "set", JSONObject().put("Enable", enabled)) != null

    fun setFirewallLevel(level: FirewallLevel): Boolean {
        val value = when (level) {
            FirewallLevel.OFF -> "off"
            FirewallLevel.MEDIUM -> "medium"
            FirewallLevel.HIGH -> "high"
        }
        return call("Firewall", "setMode", JSONObject().put("Mode", value)) != null
    }

    fun setUpnp(enabled: Boolean): Boolean =
        call("Firewall", "setUPnP", JSONObject().put("Enabled", enabled)) != null

    /** Active/désactive l'accès Internet d'un appareil (contrôle parental). */
    fun setDeviceSchedule(mac: String, enabled: Boolean, schedule: String? = null): Boolean {
        val params = JSONObject()
            .put("MACAddress", mac)
            .put("Enable", enabled)
        schedule?.let { params.put("Schedule", it) }
        return call("ParentalControl", "setSchedule", params) != null
    }
}
