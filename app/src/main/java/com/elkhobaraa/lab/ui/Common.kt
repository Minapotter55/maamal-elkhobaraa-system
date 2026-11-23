@file:OptIn(ExperimentalMaterial3Api::class)

package com.elkhobaraa.lab.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.elkhobaraa.lab.R
import com.elkhobaraa.lab.data.OrderData
import com.elkhobaraa.lab.util.LogoStore
import com.elkhobaraa.lab.util.ReportActions
import com.elkhobaraa.lab.util.ReportPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val LabPrimary = Color(0xFF991717)
val LabPrimaryDark = Color(0xFF6E0F0F)
val LabGrey = Color(0xFF3A3A3A)
val AbnormalRed = Color(0xFFE53935)
val OkGreen = Color(0xFF2E7D32)

@Composable
fun LabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = LabPrimary,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFF8E3E3),
            onPrimaryContainer = Color(0xFF5A0B0B),
            secondary = LabGrey,
            onSurface = Color(0xFF2B2B2B),
            background = Color(0xFFFAF6F6),
            surface = Color.White,
            error = AbnormalRed,
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) { content() }
        }
    }
}

@Composable
fun LogoView(size: Dp) {
    val ctx = LocalContext.current
    val custom = remember(LogoStore.version) { LogoStore.custom(ctx)?.asImageBitmap() }
    Box(
        Modifier.size(size).clip(CircleShape).background(Color.White).border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (custom != null) {
            Image(custom, null, Modifier.fillMaxSize(0.86f), contentScale = ContentScale.Fit)
        } else {
            Image(painterResource(R.drawable.lab_logo), null, Modifier.fillMaxSize(0.86f), contentScale = ContentScale.Fit)
        }
    }
}

@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)?,
    fab: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع")
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LabPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
        floatingActionButton = { fab?.invoke() },
        containerColor = MaterialTheme.colorScheme.background,
        content = content,
    )
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Icon(icon, null, tint = color)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(title, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
        color = LabPrimary, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
fun LabeledValue(label: String, value: String, valueColor: Color = Color.Unspecified, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, Modifier.weight(1f), color = Color.Gray)
        Text(value, color = valueColor, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun NumberField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { s -> onChange(s.filter { it.isDigit() || it == '.' || it == ',' || it == '٫' }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String = "حذف", onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm, color = AbnormalRed) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

@Composable
fun AmountDialog(title: String, initial: String, onConfirm: (Double) -> Unit, onDismiss: () -> Unit) {
    var v by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { NumberField(v, { v = it }, "المبلغ", Modifier.fillMaxWidth()) },
        confirmButton = {
            TextButton(onClick = {
                val a = v.replace(",", ".").replace("٫", ".").toDoubleOrNull()
                if (a != null && a > 0) { onConfirm(a); onDismiss() }
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

/** View / WhatsApp / Print buttons for an order's PDF report. [prepare] runs first (e.g. save results). */
@Composable
fun ReportButtons(load: suspend () -> OrderData?, prepare: suspend () -> Unit = {}) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    fun withReport(action: (java.io.File, OrderData) -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                prepare()
                val data = load()
                if (data == null) {
                    Toast.makeText(ctx, "الطلب غير موجود", Toast.LENGTH_SHORT).show()
                } else {
                    val f = withContext(Dispatchers.IO) { ReportPdf.create(ctx, data) }
                    action(f, data)
                }
            } catch (e: Exception) {
                Toast.makeText(ctx, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                busy = false
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("تقرير النتيجة PDF")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { withReport { f, _ -> ReportActions.view(ctx, f) } }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Visibility, null); Spacer(Modifier.width(4.dp)); Text("عرض")
            }
            Button(
                onClick = { withReport { f, d -> ReportActions.whatsapp(ctx, f, d.patient.phone) } },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
            ) {
                Icon(Icons.Filled.Share, null); Spacer(Modifier.width(4.dp)); Text("واتساب")
            }
            OutlinedButton(onClick = { withReport { f, _ -> ReportActions.print(ctx, f) } }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Print, null); Spacer(Modifier.width(4.dp)); Text("طباعة")
            }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}
