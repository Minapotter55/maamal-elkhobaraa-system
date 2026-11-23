@file:OptIn(ExperimentalMaterial3Api::class)

package com.elkhobaraa.lab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.elkhobaraa.lab.LabApp
import com.elkhobaraa.lab.util.Dates
import com.elkhobaraa.lab.util.LabInfo
import com.elkhobaraa.lab.util.money

@Composable
fun SplashScreen() {
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(LabBlue, LabBlueDark))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            LogoView(140.dp)
            Spacer(Modifier.height(20.dp))
            Text(LabInfo.NAME, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(LabInfo.TAGLINE, color = Color(0xFFDCEBFA), fontSize = 17.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(40.dp))
            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
        }
        Text(
            LabInfo.ADDRESS, color = Color(0xFFDCEBFA), fontSize = 12.sp, textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
        )
    }
}

private data class Tile(val title: String, val icon: ImageVector, val route: String)

@Composable
fun HomeScreen(nav: NavController) {
    val dao = LabApp.dao
    val start = remember { Dates.startOfDay() }
    val end = start + Dates.DAY - 1
    val patients by remember { dao.patientsBetween(start, end) }.collectAsState(0)
    val revenue by remember { dao.revenueBetween(start, end) }.collectAsState(0.0)
    val pending by remember { dao.pendingCount() }.collectAsState(0)

    val tiles = listOf(
        Tile("طلب جديد", Icons.Filled.AddCircle, "neworder"),
        Tile("المرضى", Icons.Filled.Groups, "patients"),
        Tile("التحاليل والأسعار", Icons.Filled.Science, "tests"),
        Tile("الطلبات", Icons.Filled.Receipt, "orders"),
        Tile("النتائج", Icons.Filled.Assessment, "results"),
        Tile("الحسابات", Icons.Filled.AccountBalanceWallet, "accounts"),
        Tile("الإعدادات والنسخ الاحتياطي", Icons.Filled.Settings, "settings"),
    )

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Header
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(LabBlue, LabBlueDark)), RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LogoView(64.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(LabInfo.NAME, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(LabInfo.TAGLINE, color = Color(0xFFDCEBFA), fontSize = 13.sp)
                }
            }
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("ملخص اليوم - ${Dates.date(start)}", fontWeight = FontWeight.Bold, color = LabBlue)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("مرضى النهارده", "$patients", Icons.Filled.Person, LabBlue, Modifier.weight(1f).clickableNav { nav.navigate("orders") })
                StatCard("إيراد اليوم", money(revenue), Icons.Filled.Payments, OkGreen, Modifier.weight(1f).clickableNav { nav.navigate("accounts") })
            }
            StatCard(
                "نتائج لم تُسلَّم بعد", "$pending", Icons.Filled.HourglassTop,
                if (pending > 0) AbnormalRed else OkGreen,
                Modifier.fillMaxWidth().clickableNav { nav.navigate("results") },
            )

            Spacer(Modifier.height(4.dp))
            tiles.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { t ->
                        Card(
                            onClick = { nav.navigate(t.route) },
                            modifier = Modifier.weight(1f).height(104.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp),
                        ) {
                            Column(
                                Modifier.fillMaxSize().padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(t.icon, null, tint = LabBlue, modifier = Modifier.size(34.dp))
                                Spacer(Modifier.height(6.dp))
                                Text(t.title, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, fontSize = 14.sp)
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "${LabInfo.ADDRESS}\nواتساب: ${LabInfo.WHATSAPP}  -  اتصال: ${LabInfo.PHONE}",
                color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun Modifier.clickableNav(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)
