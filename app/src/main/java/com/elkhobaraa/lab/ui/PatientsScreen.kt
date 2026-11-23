@file:OptIn(ExperimentalMaterial3Api::class)

package com.elkhobaraa.lab.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.elkhobaraa.lab.LabApp
import com.elkhobaraa.lab.data.OrderRepo
import com.elkhobaraa.lab.data.Patient
import kotlinx.coroutines.launch

@Composable
fun PatientsScreen(nav: NavController) {
    val dao = LabApp.dao
    val scope = rememberCoroutineScope()
    val patients by remember { dao.patients() }.collectAsState(emptyList())
    var q by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Patient?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Patient?>(null) }

    val filtered = patients.filter {
        q.isBlank() || it.name.contains(q.trim(), true) || it.phone.contains(q.trim()) || it.id.toString() == q.trim()
    }

    ScreenScaffold(
        "المرضى (${patients.size})", onBack = { nav.popBackStack() },
        fab = {
            ExtendedFloatingActionButton(
                onClick = { editing = null; showDialog = true },
                icon = { Icon(Icons.Filled.PersonAdd, null) },
                text = { Text("إضافة مريض") },
                containerColor = LabPrimary, contentColor = Color.White,
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            OutlinedTextField(
                q, { q = it },
                label = { Text("بحث بالاسم أو رقم التليفون أو كود المريض") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
            )
            if (filtered.isEmpty()) {
                Text("لا يوجد مرضى", Modifier.padding(24.dp).fillMaxWidth(), color = Color.Gray)
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.id }) { p ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.Bold)
                                Text(
                                    listOf("كود ${p.id}", p.age.takeIf { it.isNotBlank() }?.let { "السن $it" }, p.gender.ifBlank { null }, p.phone.ifBlank { null })
                                        .filterNotNull().joinToString("  •  "),
                                    color = Color.Gray, style = MaterialTheme.typography.bodySmall,
                                )
                                if (p.doctor.isNotBlank()) Text("د/ ${p.doctor}", color = LabPrimary, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { nav.navigate("neworder?patientId=${p.id}") }) {
                                Icon(Icons.Filled.AddCircle, "طلب جديد", tint = OkGreen)
                            }
                            IconButton(onClick = { editing = p; showDialog = true }) { Icon(Icons.Filled.Edit, "تعديل", tint = LabPrimary) }
                            IconButton(onClick = { toDelete = p }) { Icon(Icons.Filled.Delete, "حذف", tint = AbnormalRed) }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        PatientDialog(
            initial = editing,
            onSave = { p -> scope.launch { dao.upsertPatient(p) } },
            onDismiss = { showDialog = false },
        )
    }
    toDelete?.let { p ->
        ConfirmDialog(
            "حذف مريض", "هل تريد حذف ${p.name}؟ سيتم حذف كل طلباته ونتائجه.",
            onConfirm = { scope.launch { OrderRepo.deletePatient(p) } },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
fun PatientDialog(initial: Patient?, onSave: (Patient) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var age by remember { mutableStateOf(initial?.age ?: "") }
    var gender by remember { mutableStateOf(initial?.gender ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var doctor by remember { mutableStateOf(initial?.doctor ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "إضافة مريض" else "تعديل بيانات المريض") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("الاسم *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    age, { age = it }, label = { Text("السن (اختياري)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("النوع:")
                    listOf("ذكر", "أنثى").forEach { g ->
                        RadioButton(selected = gender == g, onClick = { gender = if (gender == g) "" else g })
                        Text(g)
                    }
                }
                OutlinedTextField(
                    phone, { phone = it }, label = { Text("التليفون (اختياري)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
                OutlinedTextField(doctor, { doctor = it }, label = { Text("الدكتور المحول (اختياري)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    val p = (initial ?: Patient(name = "")).copy(
                        name = name.trim(), age = age.trim(), gender = gender, phone = phone.trim(), doctor = doctor.trim(),
                    )
                    onSave(p)
                    onDismiss()
                },
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}
