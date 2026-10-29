package com.example.liveboxmanager.data

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Client de l'API REST des modems Technicolor CGA (CGA437A / CGA437AORB)
 * fournis par VOO / Orange Belgique.
 *
 * Principe (identique à l'interface web http://192.168.0.1) :
 *  1. GET /api/v1/session/menu pour amorcer le cookie PHPSESSID.
 *  2. POST /api/v1/session/login avec password "seeksalthash" → renvoie salt + saltwebui.
 *  3. challenge = PBKDF2-SHA256(mot de passe, salt) puis PBKDF2-SHA256(résultat, saltwebui).
 *  4. POST login avec le challenge → {"error":"ok"} ; le cookie "auth" sert de jeton CSRF.
 *  5. Les données se lisent sur GET /api/v1/{domaine}/{champs}?_={timestamp}
 *     et renvoient {"error":"ok","data":{...}}.
 *
 * L'app doit être sur le même réseau Wi-Fi que le modem.
 */
class LiveboxApi(
    private var host: String = "192.168.0.1",
) {
    private val cookieStore = mutableMapOf<String, String>()
    private var csrfToken: String? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                for (c in cookies) cookieStore[c.name] = c.value
            }
            override fun loadForRequest(url: HttpUrl): List<Cookie> =
                cookieStore.map { (n, v) ->
                    Cookie.Builder().name(n).value(v).domain(host).path("/").build()
                }
        })
        .build()

    val baseUrl: String get() = "http://" + host

    companion object {
        private const val UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36"
    }

    private fun now(): Long = System.currentTimeMillis()

    private fun parseJson(text: String): JSONObject =
        runCatching { JSONObject(text) }.getOrElse {
            throw IllegalStateException("Réponse inattendue du modem (pas du JSON) : " + text.take(80))
        }

    private fun httpGet(url: String): JSONObject {
        val b = Request.Builder().url(url)
            .header("User-Agent", UA)
            .header("X-Requested-With", "XMLHttpRequest")
        csrfToken?.let { b.header("X-CSRF-TOKEN", it) }
        client.newCall(b.get().build()).execute().use { r ->
            val text = r.body?.string() ?: throw IllegalStateException("Réponse vide du modem.")
            return parseJson(text)
        }
    }

    private fun postForm(url: String, vararg fields: Pair<String, String>): JSONObject {
        val fb = FormBody.Builder()
        for ((n, v) in fields) fb.add(n, v)
        val b = Request.Builder().url(url)
            .header("User-Agent", UA)
            .header("X-Requested-With", "XMLHttpRequest")
            .post(fb.build())
        csrfToken?.let { b.header("X-CSRF-TOKEN", it) }
        client.newCall(b.build()).execute().use { r ->
            val text = r.body?.string() ?: throw IllegalStateException("Réponse vide du modem.")
            return parseJson(text)
        }
    }

    /** Renvoie l'objet "data" d'un endpoint /api/v1/... (relance le login si la session a expiré). */
    private fun data(path: String): JSONObject {
        val full = baseUrl + path + (if (path.contains("?")) "&" else "?") + "_=" + now()
        var r = httpGet(full)
        if (!r.has("data")) {
            login(lastPassword, lastUsername)
            r = httpGet(full)
        }
        return r.optJSONObject("data") ?: JSONObject()
    }

    private var lastPassword: String = ""
    private var lastUsername: String = "admin"

    private fun pbkdf2(password: String, salt: String): String {
        val spec = PBEKeySpec(password.toCharArray(), salt.toByteArray(Charsets.UTF_8), 1000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hex = factory.generateSecret(spec).encoded.joinToString("") { "%02x".format(it) }
        return hex.take(32)
    }

    /** Authentifie l'utilisateur (défi PBKDF2 du firmware Technicolor). */
    fun login(password: String, username: String = "admin"): Boolean {
        lastPassword = password
        lastUsername = username
        cookieStore.clear()
        csrfToken = null

        // 1. Amorce le cookie de session, comme le fait l'interface web.
        runCatching { httpGet(baseUrl + "/api/v1/session/menu?_=" + now()) }

        // 2. Demande les sels.
        var resp = postForm(
            baseUrl + "/api/v1/session/login?_=" + now(),
            "username" to username, "password" to "seeksalthash", "logout" to "false",
        )
        val salt = resp.optString("salt")
        val saltwebui = resp.optString("saltwebui")
        if (salt.isEmpty() || saltwebui.isEmpty()) {
            val msg = resp.optString("message", resp.optString("error", ""))
            throw IllegalStateException(
                if (msg.isNotEmpty()) "Le modem refuse la connexion ($msg). Fermez l'interface web du modem sur d'autres appareils puis réessayez."
                else "Le modem n'a pas fourni de défi de connexion — session peut-être occupée."
            )
        }

        // 3. Calcule le défi.
        var challenge = pbkdf2(password, salt)
        challenge = pbkdf2(challenge, saltwebui)

        // 4. Connexion réelle.
        resp = postForm(
            baseUrl + "/api/v1/session/login?_=" + now(),
            "username" to username, "password" to challenge, "logout" to "false",
        )
        if (resp.optString("error") != "ok") {
            return false // mot de passe refusé
        }

        csrfToken = cookieStore["auth"]
        runCatching { httpGet(baseUrl + "/api/v1/session/menu?_=" + now()) }
        return true
    }

    // ---- Lecture ----

    fun fetchInfo(): LiveboxInfo {
        val res = data("/api/v1/system/ModelName,SoftwareVersion,UpTime,CMStatus,Manufacturer")
        val up = res.optString("UpTime")
        val upSeconds = Regex("\\d+").find(up)?.value?.toLongOrNull() ?: 0L
        val status = res.optString("CMStatus", "")
        return LiveboxInfo(
            model = res.optString("ModelName", "CGA437A"),
            firmwareVersion = res.optString("SoftwareVersion", ""),
            uptimeSeconds = upSeconds,
            wanStatus = if (status.equals("OPERATIONAL", ignoreCase = true)) "connecté" else status.ifEmpty { "inconnu" },
        )
    }

    fun fetchDevices(): List<Device> {
        val res = data("/api/v1/host/hostTbl,LanMode")
        val devices = mutableListOf<Device>()
        val tbl = res.opt("hostTbl")
        val entries: MutableList<JSONObject> = mutableListOf()
        when (tbl) {
            is JSONArray -> for (i in 0 until tbl.length()) {
                val e = tbl.opt(i)
                if (e is JSONObject) entries.add(e)
            }
            is JSONObject -> for (k in tbl.keys()) {
                val e = tbl.opt(k)
                if (e is JSONObject) entries.add(e)
            }
        }
        for (e in entries) {
            fun s(vararg keys: String): String {
                for (k in keys) { val v = e.optString(k, ""); if (v.isNotEmpty()) return v }
                return ""
            }
            val name = s("HostName", "hostname", "Name", "UserHostName")
            val ip = s("IPAddress", "IP", "ip")
            val mac = s("MACAddress", "MAC", "PhysAddress", "mac").ifEmpty { "?" }
            if (name.isEmpty() && ip.isEmpty()) continue
            devices.add(
                Device(
                    mac = mac,
                    name = name.ifEmpty { mac },
                    ipAddress = ip.ifEmpty { null },
                    connectionType = s("MediaType", "Type", "ConnectionType").ifEmpty { null },
                    active = !e.optString("Active", e.optString("Online", "true")).equals("false", true),
                )
            )
        }
        return devices
    }

    fun fetchWifiConfig(): WifiConfig {
        val fields = "RadioEnable,Channel,OperatingChannelBandwidth,SSID,SSIDEnable,AutoChannelEnable"
        val full = baseUrl + "/api/v1/wifi/1,2/" + fields + "?_=" + now()
        var r = httpGet(full)
        if (!r.has("1") && !r.has("2")) {
            runCatching { login(lastPassword, lastUsername) }
            r = httpGet(full)
        }
        fun radio(id: String): JSONObject =
            r.optJSONObject(id)?.optJSONObject("data") ?: JSONObject()
        val r1 = radio("1") // 2,4 GHz
        val r2 = radio("2") // 5 GHz
        return WifiConfig(
            ssid24 = r1.optString("SSID"),
            channel24 = r1.optInt("Channel", 6),
            ssid5 = r2.optString("SSID"),
            channel5 = r2.optInt("Channel", 36),
        )
    }

    fun fetchFirewall(): FirewallStatus {
        // Le firmware CGA437A n'expose pas de section pare-feu via /api/v1 :
        // on renvoie un état par défaut (l'écran affichera ces valeurs).
        return FirewallStatus()
    }

    // ---- Écriture ----

    private fun radioId(interfaceName: String): String =
        if (interfaceName.endsWith("0")) "1" else "2" // wl0 → radio 1 (2,4 GHz), wl1 → radio 2 (5 GHz)

    private fun setWifiField(interfaceName: String, field: String, value: Any): Boolean {
        if (csrfToken == null) return false
        val radio = radioId(interfaceName)
        val full = baseUrl + "/api/v1/wifi/" + radio + "/" + field + "?_=" + now()
        csrfToken?.let { }
        return runCatching {
            val resp = postForm(full, field to value.toString())
            resp.optString("error") == "ok"
        }.getOrDefault(false)
    }

    fun setWifiSsid(interfaceName: String, ssid: String): Boolean =
        setWifiField(interfaceName, "SSID", ssid)

    fun setWifiChannel(interfaceName: String, channel: Int): Boolean =
        setWifiField(interfaceName, "Channel", channel)

    fun setWifiEnabled(interfaceName: String, enabled: Boolean): Boolean =
        setWifiField(interfaceName, "RadioEnable", enabled)

    fun setFirewallLevel(level: FirewallLevel): Boolean {
        return false // non exposé par ce firmware
    }

    fun setUpnp(enabled: Boolean): Boolean {
        return false // non exposé par ce firmware
    }

    /** Active/désactive l'accès Internet d'un appareil (contrôle parental). */
    fun setDeviceSchedule(mac: String, enabled: Boolean, schedule: String? = null): Boolean {
        return false // non exposé par ce firmware
    }
}
