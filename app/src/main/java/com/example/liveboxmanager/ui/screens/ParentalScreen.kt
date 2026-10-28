package com.example.liveboxmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.liveboxmanager.data.LiveboxViewModel
import com.example.liveboxmanager.data.ParentalRule

@Composable
fun ParentalScreen(vm: LiveboxViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Contrôle parental", style = MaterialTheme.typography.headlineSmall)
        Text("Coupez l'accès Internet d'un appareil, ou planifiez-le.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))

        if (vm.parentalRules.isEmpty()) {
            Text("Aucun appareil à gérer.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vm.parentalRules, key = { it.id }) { rule -> RuleRow(rule) { vm.toggleParental(rule) } }
        }
    }
}

@Composable
private fun RuleRow(rule: ParentalRule, onToggle: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(rule.deviceName, style = MaterialTheme.typography.bodyLarge)
                Text("Planification : " + rule.schedule,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(if (rule.enabled) "Bloqué" else "Autorisé",
                style = MaterialTheme.typography.labelMedium,
                color = if (rule.enabled) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Switch(checked = rule.enabled, onCheckedChange = { onToggle() })
        }
    }
}
