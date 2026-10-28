package com.example.liveboxmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.liveboxmanager.data.LiveboxViewModel

@Composable
fun LoginScreen(vm: LiveboxViewModel, onLogged: () -> Unit) {
    var host by remember { mutableStateOf("192.168.1.1") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(vm.isLoggedIn) { if (vm.isLoggedIn) onLogged() }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Livebox Manager", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text("Configurez votre Livebox en un coup d'œil",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = host, onValueChange = { host = it },
            label = { Text("Adresse de la Livebox") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Mot de passe admin") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { vm.login(host.trim(), password) },
            enabled = !vm.isLoading && password.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (vm.isLoading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            else Text("Se connecter")
        }
        vm.message?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
