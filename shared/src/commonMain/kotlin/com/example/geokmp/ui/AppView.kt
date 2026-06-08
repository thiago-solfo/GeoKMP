package com.example.geokmp.ui

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.geokmp.data.MokoLocationRepository
import com.example.geokmp.logic.AppState
import com.example.geokmp.logic.LocationController
import dev.icerock.moko.geo.compose.BindLocationTrackerEffect
import dev.icerock.moko.geo.compose.LocationTrackerAccuracy
import dev.icerock.moko.geo.compose.rememberLocationTrackerFactory
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory

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

            // Modelo e Controller
            val repository = remember(locationTracker, permissionsController) {
                MokoLocationRepository(locationTracker, permissionsController)
            }
            val controller = remember(repository, scope) {
                LocationController(repository, scope)
            }

            // Bind dos controladores com o ciclo de vida nativo
            BindEffect(permissionsController)
            BindLocationTrackerEffect(locationTracker)

            val state by controller.state.collectAsState()
            val latitude by controller.latitude.collectAsState()
            val longitude by controller.longitude.collectAsState()
            val isGpsStarted by controller.isGpsStarted.collectAsState()
            val isPermanentlyDenied by controller.isPermanentlyDenied.collectAsState()

            when (state) {
                AppState.CheckingPermission -> {
                    Box(modifier = Modifier.fillMaxSize())
                }
                AppState.PermissionRequired -> {
                    PermissionScreen(
                        isPermanentlyDenied = isPermanentlyDenied,
                        onPermissionGranted = { controller.startTracking() },
                        onOpenSettings = { controller.openSettings() }
                    )
                }
                AppState.Dashboard -> {
                    CoordinatesDashboard(
                        lat = latitude,
                        lon = longitude,
                        isTracking = isGpsStarted,
                        onUpdate = { controller.startTracking() }
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
