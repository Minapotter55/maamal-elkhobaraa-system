package com.elkhobaraa.lab.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Patient::class, LabTest::class, LabOrder::class, OrderItem::class, Payment::class],
    version = 1,
    exportSchema = false,
)
abstract class LabDatabase : RoomDatabase() {
    abstract fun dao(): LabDao
}
