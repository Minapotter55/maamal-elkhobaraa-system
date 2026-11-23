@file:OptIn(ExperimentalMaterial3Api::class)

package com.elkhobaraa.lab.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.elkhobaraa.lab.LabApp
import com.elkhobaraa.lab.data.OrderData
import com.elkhobaraa.lab.data.OrderRepo
import com.elkhobaraa.lab.data.OrderRow
import com.elkhobaraa.lab.data.Patient
import com.elkhobaraa.lab.util.Dates
import com.elkhobaraa.lab.util.flag
import com.elkhobaraa.lab.util.money
import com.elkhobaraa.lab.util.range
import com.elkhobaraa.lab.util.toAmount
import kotlinx.coroutines.launch

@Composable
fun OrderRowCard(row: OrderRow, onClick: () -> Unit) {
    val o = row.order
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${row.patientName}", fontWeight = FontWeight.Bold)
                Text("طلب رقم ${o.id}  •  ${Dates.format(o.createdAt)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (o.delivered) "تم التسليم" else "لم يُسلَّم",
                        color = if (o.delivered) OkGreen else AbnormalRed, style = MaterialTheme.typography.bodySmall,
                    )
                    if (o.remaining > 0.001) Text("باقي ${money(o.remaining)}", color = AbnormalRed, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(money(o.net), fontWeight = FontWeight.Bold, color = LabPrimary)
        }
    }
}

@Composable
fun OrdersScreen(nav: NavController) {
    val rows by remember { LabApp.dao.orderRows() }.collectAsState(emptyList())
    var q by remember { mutableStateOf("") }
    val filtered = rows.filter {
        q.isBlank() || it.patientName.contains(q.trim(), true) || it.patientPhone.contains(q.trim()) || it.order.id.toString() == q.trim()
    }
    ScreenScaffold(
        "الطلبات", onBack = { nav.popBackStack() },
        fab = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate("neworder") },
                icon = { Icon(Icons.Filled.Add, null) }, text = { Text("طلب جديد") },
                containerColor = LabPrimary, contentColor = Color.White,
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            OutlinedTextField(
                q, { q = it }, label = { Text("بحث باسم المريض أو التليفون أو رقم الطلب") },
                leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
            )
            if (filtered.isEmpty()) Text("لا توجد طلبات", Modifier.padding(24.dp), color = Color.Gray)
            LazyColumn(
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.order.id }) { r -> OrderRowCard(r) { nav.navigate("order/${r.order.id}") } }
            }
        }
    }
}

@Composable
fun NewOrderScreen(nav: NavController, initialPatientId: Long) {
    val dao = LabApp.dao
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var patient by remember { mutableStateOf<Patient?>(null) }
    LaunchedEffect(initialPatientId) { if (initialPatientId > 0) patient = dao.patient(initialPatientId) }
    val tests by remember { dao.tests() }.collectAsState(emptyList())
    val patients by remember { dao.patients() }.collectAsState(emptyList())
    var pq by remember { mutableStateOf("") }
    var tq by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<Long>() }
    var discount by remember { mutableStateOf("") }
    var paid by remember { mutableStateOf("") }
    var showAddPatient by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var percentMode by remember { mutableStateOf(false) }
    var showExtra by remember { mutableStateOf(false) }
    var doctor by remember { mutableStateOf("") }
    var symptoms by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    LaunchedEffect(patient?.id) { patient?.doctor?.let { if (doctor.isBlank()) doctor = it } }

    val chosen = tests.filter { it.id in selected }
    val total = chosen.sumOf { it.price }
    val disc = if (percentMode) total * discount.toAmount().coerceIn(0.0, 100.0) / 100.0 else discount.toAmount()
    val net = (total - disc).coerceAtLeast(0.0)
    val pd = paid.toAmount()

    ScreenScaffold("طلب جديد", onBack = { nav.popBackStack() }) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item { SectionTitle("1) اختر المريض") }
            val p = patient
            if (p != null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Person, null, tint = LabPrimary)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.Bold)
                                Text(listOf(p.age, p.gender, p.phone).filter { it.isNotBlank() }.joinToString(" • "), style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { patient = null }) { Text("تغيير") }
                        }
                    }
                }
            } else {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            pq, { pq = it }, label = { Text("ابحث بالاسم أو التليفون") }, singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Search, null) }, modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { showAddPatient = true }) { Icon(Icons.Filled.PersonAdd, "مريض جديد", tint = LabPrimary) }
                    }
                }
                val fp = patients.filter { pq.isBlank() || it.name.contains(pq.trim(), true) || it.phone.contains(pq.trim()) }.take(6)
                if (patients.isEmpty()) item { Text("لا يوجد مرضى بعد - اضغط على أيقونة الإضافة لتسجيل مريض جديد", color = Color.Gray) }
                items(fp, key = { "p${it.id}" }) { c ->
                    Card(onClick = { patient = c }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(10.dp).fillMaxWidth()) {
                            Text(c.name, fontWeight = FontWeight.SemiBold)
                            if (c.phone.isNotBlank()) Text(c.phone, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }

            item { SectionTitle("2) اختر التحاليل (${selected.size})") }
            if (chosen.isNotEmpty()) {
                item {
                    Text(chosen.joinToString("، ") { it.name }, color = LabPrimary, style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                OutlinedTextField(
                    tq, { tq = it }, label = { Text("بحث عن تحليل") }, singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, null) }, modifier = Modifier.fillMaxWidth(),
                )
            }
            val ft = tests.filter { tq.isBlank() || it.name.contains(tq.trim(), true) }
            items(ft, key = { "t${it.id}" }) { t ->
                val on = t.id in selected
                Row(
                    Modifier.fillMaxWidth().clickable { if (on) selected.remove(t.id) else selected.add(t.id) }.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = on, onCheckedChange = { if (on) selected.remove(t.id) else selected.add(t.id) })
                    Text(t.name, Modifier.weight(1f))
                    Text(money(t.price), color = LabPrimary)
                }
            }

            item { SectionTitle("3) الحساب") }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(12.dp)) {
                        LabeledValue("الإجمالي", money(total), bold = true)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("الخصم:", color = Color.Gray)
                            FilterChip(selected = !percentMode, onClick = { percentMode = false; discount = "" }, label = { Text("مبلغ (ج.م)") })
                            FilterChip(selected = percentMode, onClick = { percentMode = true; discount = "" }, label = { Text("نسبة %") })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField(
                                discount, { discount = it },
                                if (percentMode) "نسبة الخصم % (اختياري)" else "مبلغ الخصم (اختياري)", Modifier.weight(1f),
                            )
                            NumberField(paid, { paid = it }, "المدفوع", Modifier.weight(1f))
                        }
                        if (percentMode && disc > 0) LabeledValue("قيمة الخصم", money(disc), valueColor = LabPrimary)
                        TextButton(onClick = { paid = com.elkhobaraa.lab.util.num(net) }) { Text("دفع المبلغ كامل") }
                        HorizontalDivider()
                        LabeledValue("الصافي بعد الخصم", money(net), bold = true)
                        LabeledValue("المدفوع", money(pd), valueColor = OkGreen)
                        LabeledValue("الباقي", money((net - pd).coerceAtLeast(0.0)), valueColor = AbnormalRed, bold = true)
                    }
                }
            }
            item {
                TextButton(onClick = { showExtra = !showExtra }) {
                    Icon(if (showExtra) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                    Text("4) بيانات إضافية - اختياري (الدكتور المعالج، الأعراض، ملاحظات)")
                }
            }
            if (showExtra) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(doctor, { doctor = it }, label = { Text("الدكتور المعالج / المحول (اختياري)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(symptoms, { symptoms = it }, label = { Text("الأعراض (اختياري)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(notes, { notes = it }, label = { Text("ملاحظات - تظهر في التقرير (اختياري)") }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            item {
                Button(
                    enabled = patient != null && selected.isNotEmpty() && !saving,
                    onClick = {
                        val pt = patient ?: return@Button
                        if (disc > total) { Toast.makeText(ctx, "الخصم أكبر من الإجمالي", Toast.LENGTH_SHORT).show(); return@Button }
                        saving = true
                        scope.launch {
                            val id = OrderRepo.create(pt, chosen, disc, pd, doctor.trim(), symptoms.trim(), notes.trim())
                            Toast.makeText(ctx, "تم حفظ الطلب رقم $id", Toast.LENGTH_SHORT).show()
                            nav.popBackStack()
                            nav.navigate("order/$id")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Icon(Icons.Filled.Check, null); Spacer(Modifier.width(6.dp)); Text("حفظ الطلب") }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    if (showAddPatient) {
        PatientDialog(
            initial = null,
            onSave = { np -> scope.launch { val id = dao.upsertPatient(np); patient = dao.patient(id) } },
            onDismiss = { showAddPatient = false },
        )
    }
}

@Composable
fun OrderDetailScreen(nav: NavController, id: Long) {
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    val data by produceState<OrderData?>(null, id, refresh) { value = OrderRepo.load(id) }
    var showPay by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    ScreenScaffold(
        "طلب رقم $id", onBack = { nav.popBackStack() },
        actions = { IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "حذف") } },
    ) { pad ->
        val d = data
        if (d == null) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@ScreenScaffold
        }
        val o = d.order
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(d.patient.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        LabeledValue("السن / النوع", "${d.patient.age.ifBlank { "-" }} / ${d.patient.gender.ifBlank { "-" }}")
                        LabeledValue("التليفون", d.patient.phone.ifBlank { "-" })
                        LabeledValue("الدكتور المعالج", o.doctor.ifBlank { d.patient.doctor }.ifBlank { "-" })
                        LabeledValue("التاريخ", Dates.format(o.createdAt))
                        if (o.symptoms.isNotBlank()) LabeledValue("الأعراض", o.symptoms)
                        if (o.notes.isNotBlank()) LabeledValue("ملاحظات", o.notes)
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(12.dp)) {
                        SectionTitle("التحاليل (${d.items.size})")
                        d.items.forEach { it2 ->
                            val f = it2.flag()
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                Text(it2.testName, Modifier.weight(1f))
                                Text(
                                    it2.result.ifBlank { "—" } + if (it2.unit.isNotBlank() && it2.result.isNotBlank()) " ${it2.unit}" else "",
                                    color = if (f != 0) AbnormalRed else Color.Unspecified,
                                    fontWeight = if (f != 0) FontWeight.Bold else FontWeight.Normal,
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(money(it2.price), color = Color.Gray)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Button(onClick = { nav.navigate("results/$id") }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.EditNote, null); Spacer(Modifier.width(6.dp)); Text("إدخال / تعديل النتائج")
                        }
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(12.dp)) {
                        SectionTitle("الحساب")
                        LabeledValue("الإجمالي", money(o.total))
                        LabeledValue("الخصم", money(o.discount))
                        LabeledValue("الصافي", money(o.net), bold = true)
                        LabeledValue("المدفوع", money(o.paid), valueColor = OkGreen)
                        LabeledValue("الباقي", money(o.remaining), valueColor = if (o.remaining > 0.001) AbnormalRed else OkGreen, bold = true)
                        if (o.remaining > 0.001) {
                            OutlinedButton(onClick = { showPay = true }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.Payments, null); Spacer(Modifier.width(6.dp)); Text("تحصيل مبلغ")
                            }
                        }
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("تم تسليم النتيجة للمريض", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Switch(checked = o.delivered, onCheckedChange = { v ->
                            scope.launch { OrderRepo.setDelivered(id, v); refresh++ }
                        })
                    }
                }
            }
            item { ReportButtons(load = { OrderRepo.load(id) }) }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }

    if (showPay) {
        val rem = data?.order?.remaining ?: 0.0
        AmountDialog(
            "تحصيل مبلغ", com.elkhobaraa.lab.util.num(rem),
            onConfirm = { a -> scope.launch { OrderRepo.addPayment(id, a); refresh++ } },
            onDismiss = { showPay = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            "حذف الطلب", "هل تريد حذف الطلب رقم $id ونتائجه ومدفوعاته؟",
            onConfirm = { scope.launch { OrderRepo.delete(id); nav.popBackStack() } },
            onDismiss = { confirmDelete = false },
        )
    }
}
