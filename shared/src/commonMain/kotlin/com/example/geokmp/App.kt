package com.example.geokmp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.icerock.moko.geo.compose.BindLocationTrackerEffect
import dev.icerock.moko.geo.compose.LocationTrackerAccuracy
import dev.icerock.moko.geo.compose.rememberLocationTrackerFactory
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.location.LOCATION
import kotlinx.coroutines.launch

enum class AppState {
    CheckingPermission,
    PermissionRequired,
    Dashboard
}

@Composable
fun App() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val scope = rememberCoroutineScope()
            
            // Fábricas para os controladores nativos
            val permissionsControllerFactory = rememberPermissionsControllerFactory()
            val permissionsController = remember(permissionsControllerFactory) {
                permissionsControllerFactory.createPermissionsController()
            }
            
            val locationTrackerFactory = rememberLocationTrackerFactory(LocationTrackerAccuracy.Best)
            val locationTracker = remember(locationTrackerFactory, permissionsController) {
                locationTrackerFactory.createLocationTracker(permissionsController)
            }

            // Bind dos controladores com o ciclo de vida nativo
            BindEffect(permissionsController)
            BindLocationTrackerEffect(locationTracker)

            var latitude by remember { mutableStateOf("--") }
            var longitude by remember { mutableStateOf("--") }
            var isGpsStarted by remember { mutableStateOf(false) }
            var currentAppState by remember { mutableStateOf(AppState.CheckingPermission) }
            var isPermanentlyDenied by remember { mutableStateOf(false) }

            // Lógica para iniciar o rastreamento
            val startTracking = {
                scope.launch {
                    try {
                        // Só pede permissão se ela não estiver garantida
                        if (!permissionsController.isPermissionGranted(Permission.LOCATION)) {
                            permissionsController.providePermission(Permission.LOCATION)
                        }

                        currentAppState = AppState.Dashboard
                        locationTracker.startTracking()
                        isGpsStarted = true
                        
                        locationTracker.getLocationsFlow().collect { location ->
                            latitude = location.latitude.toString()
                            longitude = location.longitude.toString()
                        }
                    } catch (e: DeniedAlwaysException) {
                        isPermanentlyDenied = true
                    } catch (e: Exception) {
                        println("Erro ao obter localização: ${e.message}")
                    }
                }
            }

            // Verifica se a permissão já foi concedida ao iniciar
            LaunchedEffect(permissionsController) {
                if (permissionsController.isPermissionGranted(Permission.LOCATION)) {
                    startTracking()
                } else {
                    currentAppState = AppState.PermissionRequired
                }
            }

            when (currentAppState) {
                AppState.CheckingPermission -> {
                    // Tela vazia durante a checagem inicial para evitar flicker
                    Box(modifier = Modifier.fillMaxSize())
                }
                AppState.PermissionRequired -> {
                    PermissionScreen(
                        isPermanentlyDenied = isPermanentlyDenied,
                        onPermissionGranted = { startTracking() },
                        onOpenSettings = { permissionsController.openAppSettings() }
                    )
                }
                AppState.Dashboard -> {
                    CoordinatesDashboard(
                        lat = latitude,
                        lon = longitude,
                        isTracking = isGpsStarted,
                        onUpdate = { startTracking() }
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionScreen(
    isPermanentlyDenied: Boolean,
    onPermissionGranted: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Precisamos da sua localização", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isPermanentlyDenied) {
            Text(
                text = "A permissão foi negada permanentemente. Por favor, ative a localização nas configurações do sistema para continuar.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onOpenSettings) {
                Text("Abrir Configurações")
            }
        } else {
            Text(
                text = "Para mostrar as coordenadas, o app precisa de permissão de GPS.",
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onPermissionGranted) {
                Text("Pedir Permissão")
            }
        }
    }
}

@Composable
fun CoordinatesDashboard(lat: String, lon: String, isTracking: Boolean, onUpdate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().safeContentPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = "Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(48.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CoordinateRow(label = "Latitude Atual", value = lat)
                Spacer(modifier = Modifier.height(16.dp))
                CoordinateRow(label = "Longitude Atual", value = lon)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        if (!isTracking) {
            Button(onClick = onUpdate) { Text("Iniciar GPS") }
        } else {
            Text("GPS Ativo e monitorando...", color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun CoordinateRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, fontWeight = FontWeight.Medium)
        Text(text = value, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}
