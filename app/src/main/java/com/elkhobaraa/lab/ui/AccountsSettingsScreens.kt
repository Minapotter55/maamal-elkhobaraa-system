@file:OptIn(ExperimentalMaterial3Api::class)

package com.elkhobaraa.lab.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.elkhobaraa.lab.data.OrderRepo
import com.elkhobaraa.lab.util.Backup
import com.elkhobaraa.lab.util.Dates
import com.elkhobaraa.lab.util.LabInfo
import com.elkhobaraa.lab.util.LogoStore
import com.elkhobaraa.lab.util.money
import com.elkhobaraa.lab.util.num
import kotlinx.coroutines.launch

@Composable
fun AccountsScreen(nav: NavController) {
    val dao = LabApp.dao
    val scope = rememberCoroutineScope()
    val day = remember { Dates.startOfDay() }
    val month = remember { Dates.startOfMonth() }
    val now = remember { System.currentTimeMillis() + Dates.DAY }
    val today by remember { dao.revenueBetween(day, day + Dates.DAY - 1) }.collectAsState(0.0)
    val monthRev by remember { dao.revenueBetween(month, now) }.collectAsState(0.0)
    val outstanding by remember { dao.outstanding() }.collectAsState(0.0)
    val rows by remember { dao.orderRows() }.collectAsState(emptyList())
    val debtors = rows.filter { it.order.remaining > 0.001 }
    var payFor by remember { mutableStateOf<Long?>(null) }

    ScreenScaffold("الحسابات", onBack = { nav.popBackStack() }) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("إيراد اليوم", money(today), Icons.Filled.Today, OkGreen, Modifier.weight(1f))
                    StatCard("إيراد الشهر", money(monthRev), Icons.Filled.CalendarMonth, LabBlue, Modifier.weight(1f))
                }
            }
            item {
                StatCard("فلوس لسه على المرضى", money(outstanding), Icons.Filled.MoneyOff, AbnormalRed, Modifier.fillMaxWidth())
            }
            item { SectionTitle("المبالغ المتبقية (${debtors.size})") }
            if (debtors.isEmpty()) item { Text("لا توجد مبالغ متبقية", color = Color.Gray) }
            items(debtors, key = { it.order.id }) { r ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(r.patientName, fontWeight = FontWeight.Bold)
                            Text("طلب ${r.order.id} • ${Dates.date(r.order.createdAt)} • ${r.patientPhone}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text("الباقي: ${money(r.order.remaining)}", color = AbnormalRed, fontWeight = FontWeight.SemiBold)
                        }
                        Button(onClick = { payFor = r.order.id }) { Text("تحصيل") }
                    }
                }
            }
        }
    }

    payFor?.let { oid ->
        val rem = rows.firstOrNull { it.order.id == oid }?.order?.remaining ?: 0.0
        AmountDialog(
            "تحصيل مبلغ - طلب $oid", num(rem),
            onConfirm = { a -> scope.launch { OrderRepo.addPayment(oid, a) } },
            onDismiss = { payFor = null },
        )
    }
}

@Composable
fun SettingsScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<android.net.Uri?>(null) }

    fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_LONG).show()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                try { toast(Backup.export(ctx, uri)) } catch (e: Exception) { toast("فشل الحفظ: ${e.message}") }
                busy = false
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImport = uri
    }
    val logoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            if (LogoStore.save(ctx, uri)) toast("تم تغيير اللوجو") else toast("تعذر قراءة الصورة")
        }
    }

    ScreenScaffold("الإعدادات", onBack = { nav.popBackStack() }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("النسخ الاحتياطي")
            Text(
                "احفظ نسخة من كل البيانات (المرضى، التحاليل، الطلبات، النتائج، الحسابات) في ملف. " +
                    "ابعت الملف لنفسك على واتساب أو Google Drive عشان لو الموبايل باظ تقدر ترجعها.",
                color = Color.Gray, style = MaterialTheme.typography.bodySmall,
            )
            Button(
                onClick = { exportLauncher.launch("khobaraa-backup-${Dates.date(System.currentTimeMillis()).replace("/", "-")}.json") },
                modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !busy,
            ) { Icon(Icons.Filled.Backup, null); Spacer(Modifier.width(6.dp)); Text("تصدير نسخة احتياطية (Export)") }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*")) },
                modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !busy,
            ) { Icon(Icons.Filled.Restore, null); Spacer(Modifier.width(6.dp)); Text("استرجاع نسخة احتياطية (Import)") }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())

            HorizontalDivider()
            SectionTitle("لوجو المعمل")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LogoView(64.dp)
                Column(Modifier.weight(1f)) {
                    Text("اللوجو بيظهر في شاشة البداية والرئيسية وأعلى تقرير النتيجة", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Row {
                        TextButton(onClick = { logoLauncher.launch("image/*") }) { Text("اختيار صورة اللوجو") }
                        TextButton(onClick = { LogoStore.reset(ctx) }) { Text("الافتراضي") }
                    }
                }
            }

            HorizontalDivider()
            SectionTitle("بيانات المعمل")
            LabeledValue("الاسم", LabInfo.NAME)
            Text(LabInfo.TAGLINE, color = LabBlue)
            LabeledValue("العنوان", "")
            Text(LabInfo.ADDRESS)
            LabeledValue("واتساب", LabInfo.WHATSAPP)
            LabeledValue("اتصال", LabInfo.PHONE)
        }
    }

    pendingImport?.let { uri ->
        ConfirmDialog(
            "استرجاع البيانات",
            "سيتم استبدال كل البيانات الحالية على الجهاز بالبيانات الموجودة في الملف. هل أنت متأكد؟",
            confirm = "استرجاع",
            onConfirm = {
                busy = true
                scope.launch {
                    try { toast(Backup.import(ctx, uri)) } catch (e: Exception) { toast("فشل الاسترجاع: ${e.message}") }
                    busy = false
                }
            },
            onDismiss = { pendingImport = null },
        )
    }
}
