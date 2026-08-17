package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.runtime.mutableLongStateOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MyApplicationApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun MyApplicationApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.INICIO) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label,
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.INICIO -> Greeting(
                    name = "Hola",
                    modifier = Modifier.padding(innerPadding)
                )

                AppDestinations.CRONOMETRO -> CronometroScreen(
                    modifier = Modifier.padding(innerPadding)
                )

                AppDestinations.CUENTA -> Text(
                    text = "Pantalla de cuenta",
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    INICIO("Inicio", R.drawable.ic_inicio),
    CRONOMETRO("Cronometro", R.drawable.ic_cronometro),
    CUENTA("Cuenta", R.drawable.ic_cuenta),
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hola $name!",
        modifier = modifier
    )
}

@Composable
fun CronometroScreen(modifier: Modifier = Modifier) {
    var elapsedMilis by rememberSaveable { mutableLongStateOf(0L)}
    var corriendo by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(corriendo) {
        val inicio = System.currentTimeMillis() - elapsedMilis
        while (corriendo) {
            delay(30.milliseconds)
            elapsedMilis = System.currentTimeMillis() - inicio
        }
    }
    val minutos = (elapsedMilis / 1000) / 60
    val segundos = (elapsedMilis / 1000) % 60
    val milisegundos = (elapsedMilis % 1000) / 10 // dos dígitos de ms

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = String.format(Locale.getDefault(),"%02d:%02d:%02d", minutos, segundos, milisegundos),
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Button(onClick = { corriendo = true }) { Text("Iniciar") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { corriendo = false }) { Text("Parar") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { elapsedMilis = 0L; corriendo = false }) { Text("Resetear") }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("Android")
    }
}