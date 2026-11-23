@file:OptIn(ExperimentalMaterial3Api::class)

package com.elkhobaraa.lab.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.elkhobaraa.lab.LabApp
import com.elkhobaraa.lab.data.LabTest
import com.elkhobaraa.lab.util.money
import com.elkhobaraa.lab.util.num
import com.elkhobaraa.lab.util.rangeText
import kotlinx.coroutines.launch

@Composable
fun TestsScreen(nav: NavController) {
    val dao = LabApp.dao
    val scope = rememberCoroutineScope()
    val tests by remember { dao.tests() }.collectAsState(emptyList())
    var q by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<LabTest?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val filtered = tests.filter { q.isBlank() || it.name.contains(q.trim(), true) }

    ScreenScaffold(
        "التحاليل والأسعار (${tests.size})", onBack = { nav.popBackStack() },
        fab = {
            ExtendedFloatingActionButton(
                onClick = { editing = null; showDialog = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("إضافة تحليل") },
                containerColor = LabPrimary, contentColor = Color.White,
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            OutlinedTextField(
                q, { q = it }, label = { Text("بحث عن تحليل") },
                leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
            )
            LazyColumn(
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(filtered, key = { it.id }) { t ->
                    Card(
                        onClick = { editing = t; showDialog = true },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(t.name, fontWeight = FontWeight.Bold)
                                val r = rangeText(t.normalMin, t.normalMax, t.normalText)
                                Text(
                                    "المعدل الطبيعي: ${r.ifBlank { "-" }} ${t.unit}",
                                    style = MaterialTheme.typography.bodySmall, color = Color.Gray,
                                )
                            }
                            Text(money(t.price), fontWeight = FontWeight.Bold, color = LabPrimary)
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        TestDialog(
            initial = editing,
            onSave = { t -> scope.launch { dao.upsertTest(t) } },
            onDelete = { t -> scope.launch { dao.deleteTest(t) } },
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
fun TestDialog(initial: LabTest?, onSave: (LabTest) -> Unit, onDelete: (LabTest) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var price by remember { mutableStateOf(initial?.price?.let { num(it) } ?: "") }
    var unit by remember { mutableStateOf(initial?.unit ?: "") }
    var min by remember { mutableStateOf(initial?.normalMin?.let { num(it) } ?: "") }
    var max by remember { mutableStateOf(initial?.normalMax?.let { num(it) } ?: "") }
    var text by remember { mutableStateOf(initial?.normalText ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }

    fun parse(s: String) = s.trim().replace(",", ".").replace("٫", ".").toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "إضافة تحليل" else "تعديل تحليل") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("اسم التحليل *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                NumberField(price, { price = it }, "السعر (ج.م) *", Modifier.fillMaxWidth())
                OutlinedTextField(unit, { unit = it }, label = { Text("الوحدة (مثال mg/dL)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NumberField(min, { min = it }, "الحد الأدنى", Modifier.weight(1f))
                    NumberField(max, { max = it }, "الحد الأقصى", Modifier.weight(1f))
                }
                OutlinedTextField(
                    text, { text = it }, label = { Text("معدل طبيعي نصي (مثال Negative)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                if (initial != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, null, tint = AbnormalRed); Text("حذف التحليل", color = AbnormalRed)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && parse(price) != null,
                onClick = {
                    onSave(
                        (initial ?: LabTest(name = "", price = 0.0)).copy(
                            name = name.trim(), price = parse(price) ?: 0.0, unit = unit.trim(),
                            normalMin = parse(min), normalMax = parse(max), normalText = text.trim(),
                        )
                    )
                    onDismiss()
                },
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )

    if (confirmDelete && initial != null) {
        ConfirmDialog(
            "حذف تحليل", "هل تريد حذف ${initial.name}؟ (الطلبات القديمة لن تتأثر)",
            onConfirm = { onDelete(initial); onDismiss() },
            onDismiss = { confirmDelete = false },
        )
    }
}
