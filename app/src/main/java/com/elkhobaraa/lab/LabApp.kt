package com.elkhobaraa.lab

import android.app.Application
import androidx.room.Room
import com.elkhobaraa.lab.data.LabDao
import com.elkhobaraa.lab.data.LabDatabase
import com.elkhobaraa.lab.data.Seed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LabApp : Application() {
    companion object {
        lateinit var db: LabDatabase
        val dao: LabDao get() = db.dao()
    }

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(this, LabDatabase::class.java, "lab.db")
            .addMigrations(LabDatabase.MIGRATION_1_2)
            .build()
        CoroutineScope(Dispatchers.IO).launch {
            if (dao.testCount() == 0) dao.insertTests(Seed.tests())
        }
    }
}
