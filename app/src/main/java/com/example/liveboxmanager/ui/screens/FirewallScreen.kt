package com.example.liveboxmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.liveboxmanager.data.FirewallLevel
import com.example.liveboxmanager.data.LiveboxViewModel

@Composable
fun FirewallScreen(vm: LiveboxViewModel) {
    var status by remember(vm.firewall) { mutableStateOf(vm.firewall) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Pare-feu", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Niveau de protection", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                FirewallLevel.entries.forEach { level ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = status.level == level,
                            onClick = { status = status.copy(level = level) }
                        )
                        Text(level.toString())
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                SettingSwitch("UPnP (jeux, NAS, impression)", status.upnpEnabled) {
                    status = status.copy(upnpEnabled = it)
                }
                SettingSwitch("Bloquer les pings externes", status.pingBlocked) {
                    status = status.copy(pingBlocked = it)
                }
                SettingSwitch("Pare-feu IPv6", status.ipv6FirewallEnabled) {
                    status = status.copy(ipv6FirewallEnabled = it)
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { vm.updateFirewall(status) },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Appliquer") }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
