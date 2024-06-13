package com.example.estacionesesqui

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.estacionesesqui.ui.theme.EstacionesEsquiTheme
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.time.LocalDate
import java.time.format.TextStyle

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            EstacionesEsquiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TFGMacroTracker()
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TFGMacroTracker() {
    val navigationController = rememberNavController()
    val viewModel: CalorieViewModel = viewModel()

    NavHost(navController = navigationController, startDestination = Routes.Pantalla1.route) {
        composable(Routes.Pantalla1.route) { Pantalla1(navigationController, viewModel) }
        composable(Routes.Pantalla5.route) { Pantalla5(navigationController, viewModel) }
        composable(Routes.Pantalla2.route) { Pantalla2(navigationController, viewModel) }
        composable("Pantalla4/{comidaNombre}",
            arguments = listOf(navArgument("comidaNombre") { type = NavType.StringType })
        ) { backStackEntry ->
            val comidaNombre = backStackEntry.arguments?.getString("comidaNombre")
            val comida = findByName(comidaNombre)
            if (comida != null) {
                Pantalla4(navigationController, comida, viewModel)
            }
        }
    }
}

@Composable
private fun findByName(comidaNombre: String?): Comida? {
    return getComidas().find { it.nombre == comidaNombre }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Pantalla1(navigationController: NavHostController, viewModel: CalorieViewModel) {
    val db =FirebaseFirestore.getInstance()
    LaunchedEffect(Unit) {
        cargarCalorias(viewModel)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val activity = LocalContext.current as Activity
        Box(
            modifier = Modifier
                .fillMaxWidth()

                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cerrar",
                tint = Color.Black,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable { activity.finish() }
            )
        }
        Text(
            text = "Macro Tracker", // Aquí va el título de tu página
            style = MaterialTheme.typography.headlineLarge, // Puedes ajustar el estilo según prefieras
            color = MaterialTheme.colorScheme.primary,
            fontSize = 60.sp
        )
        Spacer(modifier = Modifier.height(40.dp).weight(1f))
        BarChartView(consumedCalories = viewModel.consumedCalories, dailyCalories = viewModel.dailyCalories)

        Spacer(modifier = Modifier.height(40.dp).weight(1f))
        Text(
            text = "Calorías diarias: %.2f".format(viewModel.dailyCalories),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Calorías consumidas: %.2f".format(viewModel.consumedCalories),
            style = MaterialTheme.typography.bodyLarge,
            color = if (viewModel.consumedCalories > viewModel.dailyCalories) Color.Red else Color.Black
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            guardarOActualizar(viewModel)
        }) {
            Text("Guardar registro")
        }
        Button(onClick = {
            viewModel.updateDailyCalories(0F)
            viewModel.addConsumedCalories(-1*viewModel.consumedCalories)
            reiniciarYBorrarRegistro(viewModel)

        }) {
            Text("Reiniciar")
        }
        Spacer(modifier = Modifier.height(32.dp))






        Button(onClick = {
            navigationController.navigate(Routes.Pantalla5.route)
        }) {
            Text("Seleccionar Comida")
        }
        Button(onClick = {
            navigationController.navigate(Routes.Pantalla2.route)
        }) {
            Text("Calcular calorias ")
        }
        Spacer(modifier = Modifier.height(16.dp))




        Spacer(modifier = Modifier.height(40.dp).weight(1f))

    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun reiniciarYBorrarRegistro(viewModel: CalorieViewModel) {
    val db = FirebaseFirestore.getInstance()
    val currentDate = LocalDate.now().toString()
    val documentRef = db.collection("registros").document(currentDate)

    // Verificar si las calorías consumidas son cero
    if (viewModel.consumedCalories == 0F) {
        // Si las calorías consumidas son cero, borrar el registro de la fecha actual
        documentRef.delete()
            .addOnSuccessListener {
                // Documento eliminado exitosamente
                println("Registro de hoy eliminado con éxito.")
            }
            .addOnFailureListener { e ->
                // Manejar error
                println("Error eliminando el registro de hoy: $e")
            }
    } else {
        // Si las calorías consumidas no son cero, actualizar o crear el registro como de costumbre
        val consumedData = hashMapOf("registro diario" to viewModel.consumedCalories)
        documentRef.set(consumedData)
            .addOnSuccessListener {
                // Documento creado o actualizado exitosamente
                println("Registro actualizado o creado con éxito.")
            }
            .addOnFailureListener { e ->
                // Manejar error
                println("Error actualizando o creando el registro: $e")
            }
    }
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BarChartView(consumedCalories: Float, dailyCalories: Float) {
    val progress = (consumedCalories / dailyCalories).coerceIn(0f, 1f)
    val progressColor = if (consumedCalories > dailyCalories) Color.Red else Color.Green

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            val barWidth = size.width
            val barHeight = size.height
            val progressWidth = barWidth * progress


            drawRect(
                color = Color.LightGray,
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
            )


            drawRect(
                color = progressColor,
                size = androidx.compose.ui.geometry.Size(progressWidth, barHeight)
            )
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Consumidas: %.2f".format(consumedCalories), fontSize = 14.sp)
            Text(text = "Diarias: %.2f".format(dailyCalories),fontSize = 14.sp)
        }
    }
}
@RequiresApi(Build.VERSION_CODES.O)
fun cargarCalorias(viewModel: CalorieViewModel) {
    val db = FirebaseFirestore.getInstance()
    val currentDate = LocalDate.now().toString()
    val registroRef = db.collection("registros").document(currentDate)
    val caloriasDiariasRef = db.collection("calorias_diarias").document("daily_calories")

    // Cargar registro diario de calorías consumidas
    registroRef.get().addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val document = task.result
            if (document != null && document.exists()) {
                val currentCalories = document.getLong("registro diario") ?: 0L
                viewModel.consumedCalories= currentCalories.toFloat()
            } else {
                // Si el documento no existe, inicializar con 0
                viewModel.addConsumedCalories(0F)
            }
        } else {
            // Manejar error al obtener el documento
            println("Error obteniendo el documento de registros: ${task.exception}")
        }
    }

    // Cargar calorías diarias
    caloriasDiariasRef.get().addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val document = task.result
            if (document != null && document.exists()) {
                val dailyCalories = document.getLong("calorias diarias") ?: 0L
                viewModel.updateDailyCalories(dailyCalories.toFloat())
            } else {
                // Si el documento no existe, inicializar con un valor por defecto
                viewModel.updateDailyCalories(0F)
            }
        } else {
            // Manejar error al obtener el documento
            println("Error obteniendo el documento de calorías diarias: ${task.exception}")
        }
    }
}
@RequiresApi(Build.VERSION_CODES.O)
fun guardarOActualizar(viewModel: CalorieViewModel){
    val db = FirebaseFirestore.getInstance()
    val currentDate = LocalDate.now().toString()
    val documentRef = db.collection("registros").document(currentDate)
    val dailyCaloriesRef = db.collection("calorias_diarias").document("daily_calories")

    // Datos a actualizar para las calorías consumidas
    val consumedData = hashMapOf("registro diario" to viewModel.consumedCalories)

    // Datos a actualizar para las calorías diarias
    val dailyCaloriesData = hashMapOf("calorias diarias" to viewModel.dailyCalories)

    // Verificar si el documento de calorías consumidas existe
    documentRef.get().addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val document = task.result
            if (document != null && document.exists()) {
                // Si el documento existe, obtener el valor actual
                val currentCalories = document.getLong("registro diario") ?: 0L
                // Sumar el nuevo valor al valor actual
                val updatedCalories =  viewModel.consumedCalories
                // Actualizar el documento con el nuevo valor
                documentRef.update("registro diario", updatedCalories)
                    .addOnSuccessListener {
                        // Documento actualizado exitosamente
                        println("Registro actualizado con éxito.")
                    }
                    .addOnFailureListener { e ->
                        // Manejar error
                        println("Error actualizando el registro: $e")
                    }
            } else {
                // Si el documento no existe, crear el documento con el nuevo valor
                documentRef.set(consumedData)
                    .addOnSuccessListener {
                        // Documento creado exitosamente
                        println("Registro creado con éxito.")
                    }
                    .addOnFailureListener { e ->
                        // Manejar error
                        println("Error creando el registro: $e")
                    }
            }
        } else {
            // Manejar error al obtener el documento
            println("Error obteniendo el documento: ${task.exception}")
        }
    }

    // Actualizar o crear el documento de calorías diarias
    dailyCaloriesRef.set(dailyCaloriesData)
        .addOnSuccessListener {
            // Documento creado o actualizado exitosamente
            println("Calorías diarias actualizadas con éxito.")
        }
        .addOnFailureListener { e ->
            // Manejar error
            println("Error actualizando las calorías diarias: $e")
        }
}

@Composable
fun Pantalla5(navigationController: NavHostController, viewModel: CalorieViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val activity = LocalContext.current as Activity
        Box(
            modifier = Modifier
                .fillMaxWidth()

                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cerrar",
                tint = Color.Black,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable { activity.finish() }
            )
        }

        Text(
            text = "Calorías diarias: %.2f".format(viewModel.dailyCalories),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Calorías consumidas: %.2f".format(viewModel.consumedCalories),
            style = MaterialTheme.typography.bodyLarge,
            color = if (viewModel.consumedCalories > viewModel.dailyCalories) Color.Red else Color.Black
        )

        Row {
            Button(onClick = { navigationController.navigate(Routes.Pantalla1.route) }) {
                Text(text = "Menu")
            }
        }
        ComidasVista(navigationController)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun Pantalla4(navigationController: NavHostController, comida: Comida, viewModel: CalorieViewModel) {
    var cantidad by remember { mutableStateOf("") }
    var totalCalories by remember { mutableStateOf(0.0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val activity = LocalContext.current as Activity
        Box(
            modifier = Modifier
                .fillMaxWidth()

                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cerrar",
                tint = Color.Black,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable { activity.finish() }
            )
        }
        Text(
            text = comida.nombre, // Aquí va el título de tu página
            style = MaterialTheme.typography.headlineLarge, // Puedes ajustar el estilo según prefieras

            fontSize = 40.sp

        )
        Card(
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            colors = CardDefaults.cardColors(Color.White),
            modifier = Modifier
                .width(200.dp)
                .height(200.dp),
        ) {
            Image(
                painter = painterResource(id = comida.logo),
                contentDescription = "logo comida",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = cantidad,
            onValueChange = { cantidad = it },
            label = { Text("Cantidad (g)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done),



        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val cantidadValue = cantidad.toFloatOrNull() ?: 0f
            totalCalories = (comida.calorias / 100f) * cantidadValue
            viewModel.addConsumedCalories(totalCalories)
            guardarOActualizar(viewModel)
        }) {
            Text("Agregar")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Total calorías de esta comida: %.2f".format(totalCalories))

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Calorías diarias: %.2f".format(viewModel.dailyCalories),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Calorías consumidas: %.2f".format(viewModel.consumedCalories),
            style = MaterialTheme.typography.bodyLarge,
            color = if (viewModel.consumedCalories > viewModel.dailyCalories) Color.Red  else Color.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            navigationController.navigate(Routes.Pantalla1.route)
        }) {
            Text("Menu")
        }

        Button(onClick = {
            navigationController.navigate(Routes.Pantalla5.route)
        }) {
            Text("Seleccionar otra comida")
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Pantalla2(navigationController: NavHostController, viewModel: CalorieViewModel) {
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var result by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val activity = LocalContext.current as Activity
        Box(
            modifier = Modifier
                .fillMaxWidth()

                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cerrar",
                tint = Color.Black,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable { activity.finish() }
            )
        }
        Text(
            text = "Consumo calorico diario",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 40.sp

        )
        Spacer(modifier = Modifier.height(16.dp).weight(1f))

        TextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text("Peso (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = height,
            onValueChange = { height = it },
            label = { Text("Altura (cm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = age,
            onValueChange = { age = it },
            label = { Text("Edad") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            RadioButton(
                selected = gender == "Male",
                onClick = { gender = "Male" }
            )
            Text("Hombre")

            Spacer(modifier = Modifier.width(16.dp))

            RadioButton(
                selected = gender == "Female",
                onClick = { gender = "Female" }
            )
            Text("Mujer")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val weightValue = weight.toFloatOrNull() ?: 0f
            val heightValue = height.toFloatOrNull() ?: 0f
            val ageValue = age.toIntOrNull() ?: 0

            val tmb = if (gender == "Male") {
                88.362 + (13.397 * weightValue) + (4.799 * heightValue) - (5.677 * ageValue)
            } else {
                447.593 + (9.247 * weightValue) + (3.098 * heightValue) - (4.330 * ageValue)
            }

            result = "Calorías diarias: %.2f".format(tmb)
            viewModel.updateDailyCalories(tmb.toFloat())
            guardarOActualizar(viewModel)
        }) {
            Text("Calcular")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = result, style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            navigationController.navigate(Routes.Pantalla1.route)
        }) {
            Text("Menu")
        }
        Spacer(modifier = Modifier.height(16.dp).weight(1f))

    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    EstacionesEsquiTheme {
        //TFGMacroTracker(caloriasDiarias, caloriasConsumidas)
    }
}

@Composable
fun ComidasVista(navigationController: NavHostController) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(10.dp)
    ) {
        items(getComidas()) {
            ItemComida(it, navigationController)
        }
    }
}

sealed class Routes(val route: String) {
    object Pantalla1 : Routes("Pantalla1")
    object Pantalla2 : Routes("Pantalla2")
    object Pantalla5 : Routes("Pantalla5")
}



fun getComidas(): List<Comida> {
    return listOf(
        Comida("Garbanzo", 120, R.drawable._01_garbanzo),
        Comida("Fruta", 60, R.drawable._02_comida_sana),
        Comida("Pizza", 320, R.drawable._03_pizza),
        Comida("Arroz", 129, R.drawable._04_arroz),
        Comida("Verdura", 50, R.drawable._05_vegetales),
        Comida("Patatas fritas", 273, R.drawable._06_papas_fritas),
        Comida("Carne", 120, R.drawable._07_carne),
        Comida("Pescado", 110, R.drawable.pescado),
        Comida("Pollo", 110, R.drawable._09_pierna_de_pollo),
        Comida("Pasta", 160, R.drawable._10_pasta),
        Comida("Hamburguesa", 235, R.drawable._11_burguer),
        Comida("Snacks", 300, R.drawable._12_patatas_fritas),
        Comida("Aceite de oliva", 884, R.drawable._13_aceite_de_oliva),
        Comida("Aguacate", 160, R.drawable._14_palta),
        Comida("Frutos secos", 600, R.drawable._15_tuerca),
        Comida("Refrescos", 100, R.drawable._16_refresco)
    )
}

class CalorieViewModel : ViewModel() {
    var dailyCalories by mutableStateOf(0f)
        private set

    var consumedCalories by mutableStateOf(0f)

    fun updateDailyCalories(calories: Float) {
        dailyCalories = calories
    }

    fun addConsumedCalories(calories: Float) {
        consumedCalories += calories
    }


}

data class Comida(
    var nombre: String,
    var calorias: Int,
    @DrawableRes var logo: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemComida(comida: Comida, navigationController: NavHostController) {
    Card(
        //modifier=Modifier.height(64.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        colors = CardDefaults.cardColors(Color.White),
        onClick = { navigationController.navigate("Pantalla4/${comida.nombre}") }
    ) {
        Image(
            painter = painterResource(id = comida.logo),
            contentDescription = "logo estación",
            modifier = Modifier
                .fillMaxWidth().height(110.dp).padding(5.dp),

            contentScale = ContentScale.Crop,
        )
    }
}
