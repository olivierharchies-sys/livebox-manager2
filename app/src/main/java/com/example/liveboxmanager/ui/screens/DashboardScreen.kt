package com.example.liveboxmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.liveboxmanager.data.Device
import com.example.liveboxmanager.data.LiveboxViewModel

@Composable
fun DashboardScreen(vm: LiveboxViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tableau de bord", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { vm.refreshAll() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Actualiser")
            }
        }
        Spacer(Modifier.height(12.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(vm.info.model, style = MaterialTheme.typography.titleMedium)
                Text("Firmware : " + vm.info.firmwareVersion,
                    style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Circle,
                        contentDescription = null,
                        tint = if (vm.info.wanStatus == "connecté") Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Internet : " + vm.info.wanStatus)
                    Spacer(Modifier.weight(1f))
                    Text("${vm.devices.count { it.active }} appareil(s) actif(s)")
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Text("Appareils connectés", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (vm.devices.isEmpty()) {
            Text("Aucun appareil détecté.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vm.devices, key = { it.mac }) { device -> DeviceRow(device) }
        }
    }
}

@Composable
private fun DeviceRow(device: Device) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when (device.connectionType?.lowercase()) {
                    "ethernet" -> Icons.Filled.Lan
                    "plc" -> Icons.Filled.Cable
                    else -> Icons.Filled.Wifi
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(device.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    listOfNotNull(device.ipAddress, device.mac).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (device.active) {
                AssistChip(onClick = {}, label = { Text("En ligne") },
                    leadingIcon = { Icon(Icons.Filled.Check, null, Modifier.size(14.dp)) })
            }
        }
    }
}
