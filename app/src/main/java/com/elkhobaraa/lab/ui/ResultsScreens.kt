@file:OptIn(ExperimentalMaterial3Api::class)

package com.elkhobaraa.lab.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.elkhobaraa.lab.LabApp
import com.elkhobaraa.lab.data.OrderItem
import com.elkhobaraa.lab.data.OrderRepo
import com.elkhobaraa.lab.util.flag
import com.elkhobaraa.lab.util.range
import kotlinx.coroutines.launch

@Composable
fun ResultsListScreen(nav: NavController) {
    val rows by remember { LabApp.dao.orderRows() }.collectAsState(emptyList())
    var showAll by remember { mutableStateOf(false) }
    val list = if (showAll) rows else rows.filter { !it.order.delivered }
    ScreenScaffold("النتائج", onBack = { nav.popBackStack() }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !showAll, onClick = { showAll = false }, label = { Text("لم تُسلَّم (${rows.count { !it.order.delivered }})") })
                FilterChip(selected = showAll, onClick = { showAll = true }, label = { Text("الكل (${rows.size})") })
            }
            if (list.isEmpty()) Text("لا توجد نتائج معلقة 👍", Modifier.padding(24.dp), color = Color.Gray)
            LazyColumn(
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(list, key = { it.order.id }) { r -> OrderRowCard(r) { nav.navigate("results/${r.order.id}") } }
            }
        }
    }
}

@Composable
fun ResultsScreen(nav: NavController, id: Long) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val items = remember { mutableStateListOf<OrderItem>() }
    var patientName by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        val d = OrderRepo.load(id)
        if (d != null) {
            items.clear(); items.addAll(d.items)
            patientName = d.patient.name
        }
        loaded = true
    }

    suspend fun save() {
        LabApp.dao.updateItems(items.toList())
    }

    ScreenScaffold(
        "نتائج: $patientName", onBack = { nav.popBackStack() },
        actions = {
            IconButton(onClick = {
                scope.launch { save(); Toast.makeText(ctx, "تم حفظ النتائج", Toast.LENGTH_SHORT).show() }
            }) { Icon(Icons.Filled.Save, "حفظ") }
        },
    ) { pad ->
        if (!loaded) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@ScreenScaffold
        }
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(items, key = { _, it -> it.id }) { idx, item ->
                val f = item.flag()
                val color = if (f != 0) AbnormalRed else LabGrey
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (f != 0) Color(0xFFFFEBEE) else Color.White),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.testName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            if (f != 0) {
                                Text(if (f > 0) "مرتفع ↑" else "منخفض ↓", color = AbnormalRed, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = item.result,
                            onValueChange = { v -> items[idx] = item.copy(result = v) },
                            label = { Text("النتيجة") },
                            suffix = { if (item.unit.isNotBlank()) Text(item.unit) },
                            isError = f != 0,
                            singleLine = false,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                errorBorderColor = AbnormalRed, errorLabelColor = AbnormalRed, errorTextColor = AbnormalRed,
                            ),
                        )
                        val r = item.range()
                        if (r.isNotBlank()) {
                            Text("المعدل الطبيعي: $r ${item.unit}", style = MaterialTheme.typography.bodySmall, color = color)
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        scope.launch {
                            save()
                            Toast.makeText(ctx, "تم حفظ النتائج", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) { Icon(Icons.Filled.Save, null); Spacer(Modifier.width(6.dp)); Text("حفظ النتائج") }
            }
            item {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            save(); OrderRepo.setDelivered(id, true)
                            Toast.makeText(ctx, "تم الحفظ وتسجيل التسليم", Toast.LENGTH_SHORT).show()
                            nav.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Icon(Icons.Filled.CheckCircle, null, tint = OkGreen); Spacer(Modifier.width(6.dp)); Text("حفظ وتسجيل تسليم النتيجة") }
            }
            item { ReportButtons(load = { OrderRepo.load(id) }, prepare = { save() }) }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
}
