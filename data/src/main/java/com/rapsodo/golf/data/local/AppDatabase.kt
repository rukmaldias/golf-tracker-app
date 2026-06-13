package com.rapsodo.golf.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rapsodo.golf.data.local.dao.PlayerDao
import com.rapsodo.golf.data.local.entity.PlayerEntity
import com.rapsodo.golf.data.local.entity.ShotEntity

@Database(
    entities = [PlayerEntity::class, ShotEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
}