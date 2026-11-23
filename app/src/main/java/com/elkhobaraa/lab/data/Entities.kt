package com.elkhobaraa.lab.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val age: String = "",
    val gender: String = "",
    val phone: String = "",
    val doctor: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "tests")
data class LabTest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val price: Double,
    val unit: String = "",
    val normalMin: Double? = null,
    val normalMax: Double? = null,
    val normalText: String = "",
)

@Entity(tableName = "orders")
data class LabOrder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val total: Double,
    val discount: Double = 0.0,
    val paid: Double = 0.0,
    val delivered: Boolean = false,
    val doctor: String = "",
    val symptoms: String = "",
    val notes: String = "",
) {
    val net: Double get() = (total - discount).coerceAtLeast(0.0)
    val remaining: Double get() = (net - paid).coerceAtLeast(0.0)
}

/** Snapshot of the test at order time, so editing a test later does not change old reports. */
@Entity(tableName = "order_items")
data class OrderItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val testId: Long,
    val testName: String,
    val price: Double,
    val unit: String = "",
    val normalMin: Double? = null,
    val normalMax: Double? = null,
    val normalText: String = "",
    val result: String = "",
)

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
)

data class OrderRow(
    @Embedded val order: LabOrder,
    val patientName: String,
    val patientPhone: String,
)

data class OrderData(val order: LabOrder, val patient: Patient, val items: List<OrderItem>)
