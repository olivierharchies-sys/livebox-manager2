package com.example.liveboxmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.liveboxmanager.data.LiveboxViewModel

@Composable
fun WifiScreen(vm: LiveboxViewModel) {
    var config by remember(vm.wifi) { mutableStateOf(vm.wifi) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Paramètres WiFi", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Bande 2,4 GHz", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = config.ssid24, onValueChange = { config = config.copy(ssid24 = it) },
                    label = { Text("Nom du réseau (SSID)") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Canal : ${config.channel24}", Modifier.weight(1f))
                    Slider(
                        value = config.channel24.toFloat(),
                        onValueChange = { config = config.copy(channel24 = it.toInt()) },
                        valueRange = 1f..13f, steps = 11
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Bande 5 GHz", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = config.ssid5, onValueChange = { config = config.copy(ssid5 = it) },
                    label = { Text("Nom du réseau (SSID)") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Canal : ${config.channel5}", Modifier.weight(1f))
                    Slider(
                        value = config.channel5.toFloat(),
                        onValueChange = { config = config.copy(channel5 = it.toInt()) },
                        valueRange = 36f..140f
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        Button(onClick = { vm.updateWifi(config) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Appliquer les modifications")
        }
        vm.message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.primary)
        }
    }
}
