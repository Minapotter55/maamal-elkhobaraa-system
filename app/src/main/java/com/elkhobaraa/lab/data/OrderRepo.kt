package com.elkhobaraa.lab.data

import androidx.room.withTransaction
import com.elkhobaraa.lab.LabApp

object OrderRepo {
    private val db get() = LabApp.db
    private val dao get() = LabApp.dao

    suspend fun create(p: Patient, tests: List<LabTest>, discount: Double, paid: Double): Long =
        db.withTransaction {
            val now = System.currentTimeMillis()
            val id = dao.insertOrder(
                LabOrder(patientId = p.id, createdAt = now, total = tests.sumOf { it.price }, discount = discount, paid = paid)
            )
            dao.insertItems(tests.map {
                OrderItem(
                    orderId = id, testId = it.id, testName = it.name, price = it.price, unit = it.unit,
                    normalMin = it.normalMin, normalMax = it.normalMax, normalText = it.normalText,
                )
            })
            if (paid > 0) dao.insertPayment(Payment(orderId = id, amount = paid, date = now))
            id
        }

    suspend fun load(id: Long): OrderData? {
        val o = dao.order(id) ?: return null
        val p = dao.patient(o.patientId) ?: return null
        return OrderData(o, p, dao.items(id))
    }

    suspend fun addPayment(orderId: Long, amount: Double) {
        db.withTransaction {
            val o = dao.order(orderId) ?: return@withTransaction
            dao.updateOrder(o.copy(paid = o.paid + amount))
            dao.insertPayment(Payment(orderId = orderId, amount = amount))
        }
    }

    suspend fun setDelivered(orderId: Long, delivered: Boolean) {
        val o = dao.order(orderId) ?: return
        dao.updateOrder(o.copy(delivered = delivered))
    }

    suspend fun delete(orderId: Long) {
        db.withTransaction {
            val o = dao.order(orderId) ?: return@withTransaction
            dao.deleteItemsOf(o.id)
            dao.deletePaymentsOf(o.id)
            dao.deleteOrder(o)
        }
    }

    suspend fun deletePatient(p: Patient) {
        db.withTransaction {
            for (o in dao.ordersOf(p.id)) delete(o.id)
            dao.deletePatient(p)
        }
    }
}
