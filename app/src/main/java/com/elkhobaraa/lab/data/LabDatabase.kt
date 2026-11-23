package com.elkhobaraa.lab.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Patient::class, LabTest::class, LabOrder::class, OrderItem::class, Payment::class],
    version = 2,
    exportSchema = false,
)
abstract class LabDatabase : RoomDatabase() {
    abstract fun dao(): LabDao

    companion object {
        /** v2: optional doctor / symptoms / notes per order. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE orders ADD COLUMN doctor TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE orders ADD COLUMN symptoms TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE orders ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
