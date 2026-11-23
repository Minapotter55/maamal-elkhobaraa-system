package com.elkhobaraa.lab.util

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.elkhobaraa.lab.LabApp
import com.elkhobaraa.lab.data.LabOrder
import com.elkhobaraa.lab.data.LabTest
import com.elkhobaraa.lab.data.OrderItem
import com.elkhobaraa.lab.data.Patient
import com.elkhobaraa.lab.data.Payment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Full backup of the database to a single JSON file, and restore from it. */
object Backup {
    private fun JSONObject.optD(k: String): Double? = if (isNull(k)) null else getDouble(k)
    private fun JSONObject.putD(k: String, v: Double?): JSONObject = put(k, v ?: JSONObject.NULL)

    suspend fun export(ctx: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val d = LabApp.dao
        val patients = d.allPatients()
        val root = JSONObject()
            .put("app", "maamal-elkhobaraa")
            .put("version", 1)
            .put("exportedAt", System.currentTimeMillis())
            .put("patients", JSONArray(patients.map {
                JSONObject().put("id", it.id).put("name", it.name).put("age", it.age).put("gender", it.gender)
                    .put("phone", it.phone).put("doctor", it.doctor).put("createdAt", it.createdAt)
            }))
            .put("tests", JSONArray(d.allTests().map {
                JSONObject().put("id", it.id).put("name", it.name).put("price", it.price).put("unit", it.unit)
                    .putD("normalMin", it.normalMin).putD("normalMax", it.normalMax).put("normalText", it.normalText)
            }))
            .put("orders", JSONArray(d.allOrders().map {
                JSONObject().put("id", it.id).put("patientId", it.patientId).put("createdAt", it.createdAt)
                    .put("total", it.total).put("discount", it.discount).put("paid", it.paid).put("delivered", it.delivered)
                    .put("doctor", it.doctor).put("symptoms", it.symptoms).put("notes", it.notes)
            }))
            .put("items", JSONArray(d.allItems().map {
                JSONObject().put("id", it.id).put("orderId", it.orderId).put("testId", it.testId).put("testName", it.testName)
                    .put("price", it.price).put("unit", it.unit).putD("normalMin", it.normalMin).putD("normalMax", it.normalMax)
                    .put("normalText", it.normalText).put("result", it.result)
            }))
            .put("payments", JSONArray(d.allPayments().map {
                JSONObject().put("id", it.id).put("orderId", it.orderId).put("amount", it.amount).put("date", it.date)
            }))
        ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(root.toString().toByteArray(Charsets.UTF_8)) }
        "تم حفظ النسخة الاحتياطية (${patients.size} مريض)"
    }

    suspend fun import(ctx: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val txt = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
        val root = JSONObject(txt)
        require(root.optString("app") == "maamal-elkhobaraa") { "الملف ليس نسخة احتياطية من التطبيق" }
        fun arr(k: String): List<JSONObject> {
            val a = root.optJSONArray(k) ?: JSONArray()
            return (0 until a.length()).map { a.getJSONObject(it) }
        }
        val patients = arr("patients").map {
            Patient(it.getLong("id"), it.getString("name"), it.optString("age"), it.optString("gender"),
                it.optString("phone"), it.optString("doctor"), it.getLong("createdAt"))
        }
        val tests = arr("tests").map {
            LabTest(it.getLong("id"), it.getString("name"), it.getDouble("price"), it.optString("unit"),
                it.optD("normalMin"), it.optD("normalMax"), it.optString("normalText"))
        }
        val orders = arr("orders").map {
            LabOrder(it.getLong("id"), it.getLong("patientId"), it.getLong("createdAt"), it.getDouble("total"),
                it.optDouble("discount", 0.0), it.optDouble("paid", 0.0), it.optBoolean("delivered"),
                it.optString("doctor"), it.optString("symptoms"), it.optString("notes"))
        }
        val items = arr("items").map {
            OrderItem(it.getLong("id"), it.getLong("orderId"), it.getLong("testId"), it.getString("testName"),
                it.getDouble("price"), it.optString("unit"), it.optD("normalMin"), it.optD("normalMax"),
                it.optString("normalText"), it.optString("result"))
        }
        val payments = arr("payments").map {
            Payment(it.getLong("id"), it.getLong("orderId"), it.getDouble("amount"), it.getLong("date"))
        }
        val d = LabApp.dao
        LabApp.db.withTransaction {
            d.clearItems(); d.clearPayments(); d.clearOrders(); d.clearPatients(); d.clearTests()
            d.insertPatients(patients); d.insertTests(tests); d.insertOrders(orders)
            d.insertAllItems(items); d.insertPayments(payments)
        }
        "تم استرجاع البيانات: ${patients.size} مريض، ${orders.size} طلب، ${tests.size} تحليل"
    }
}
