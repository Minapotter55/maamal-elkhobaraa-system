package com.elkhobaraa.lab.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LabDao {
    // ---- Patients
    @Query("SELECT * FROM patients ORDER BY id DESC")
    fun patients(): Flow<List<Patient>>

    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun patient(id: Long): Patient?

    @Upsert
    suspend fun upsertPatient(p: Patient): Long

    @Delete
    suspend fun deletePatient(p: Patient)

    // ---- Tests
    @Query("SELECT * FROM tests ORDER BY name COLLATE NOCASE")
    fun tests(): Flow<List<LabTest>>

    @Upsert
    suspend fun upsertTest(t: LabTest): Long

    @Delete
    suspend fun deleteTest(t: LabTest)

    @Query("SELECT COUNT(*) FROM tests")
    suspend fun testCount(): Int

    // ---- Orders
    @Insert
    suspend fun insertOrder(o: LabOrder): Long

    @Update
    suspend fun updateOrder(o: LabOrder)

    @Delete
    suspend fun deleteOrder(o: LabOrder)

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun order(id: Long): LabOrder?

    @Query("SELECT * FROM orders WHERE patientId = :pid")
    suspend fun ordersOf(pid: Long): List<LabOrder>

    @Query(
        "SELECT o.*, p.name AS patientName, p.phone AS patientPhone FROM orders o " +
            "JOIN patients p ON p.id = o.patientId ORDER BY o.id DESC"
    )
    fun orderRows(): Flow<List<OrderRow>>

    @Insert
    suspend fun insertItems(items: List<OrderItem>)

    @Update
    suspend fun updateItems(items: List<OrderItem>)

    @Query("SELECT * FROM order_items WHERE orderId = :orderId ORDER BY id")
    suspend fun items(orderId: Long): List<OrderItem>

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteItemsOf(orderId: Long)

    @Insert
    suspend fun insertPayment(p: Payment)

    @Query("DELETE FROM payments WHERE orderId = :orderId")
    suspend fun deletePaymentsOf(orderId: Long)

    // ---- Stats
    @Query("SELECT COUNT(DISTINCT patientId) FROM orders WHERE createdAt BETWEEN :from AND :to")
    fun patientsBetween(from: Long, to: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE date BETWEEN :from AND :to")
    fun revenueBetween(from: Long, to: Long): Flow<Double>

    @Query("SELECT COUNT(*) FROM orders WHERE delivered = 0")
    fun pendingCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(total - discount - paid), 0) FROM orders WHERE total - discount - paid > 0.001")
    fun outstanding(): Flow<Double>

    // ---- Backup
    @Query("SELECT * FROM patients") suspend fun allPatients(): List<Patient>
    @Query("SELECT * FROM tests") suspend fun allTests(): List<LabTest>
    @Query("SELECT * FROM orders") suspend fun allOrders(): List<LabOrder>
    @Query("SELECT * FROM order_items") suspend fun allItems(): List<OrderItem>
    @Query("SELECT * FROM payments") suspend fun allPayments(): List<Payment>

    @Query("DELETE FROM patients") suspend fun clearPatients()
    @Query("DELETE FROM tests") suspend fun clearTests()
    @Query("DELETE FROM orders") suspend fun clearOrders()
    @Query("DELETE FROM order_items") suspend fun clearItems()
    @Query("DELETE FROM payments") suspend fun clearPayments()

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPatients(l: List<Patient>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTests(l: List<LabTest>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertOrders(l: List<LabOrder>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAllItems(l: List<OrderItem>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPayments(l: List<Payment>)
}
