package com.example.cassette.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "config")
data class ConfigEntity(
    @PrimaryKey
    val key: String,
    @ColumnInfo(name = "value")
    val value: String
)
