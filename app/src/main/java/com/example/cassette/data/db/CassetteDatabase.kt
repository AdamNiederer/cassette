package com.example.cassette.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TrackEntity::class, AlbumEntity::class, ConfigEntity::class], version = 9, exportSchema = false)

abstract class CassetteDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun albumDao(): AlbumDao
    abstract fun configDao(): ConfigDao
}
