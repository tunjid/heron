package com.tunjid.heron.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tunjid.heron.data.local.db.AppDatabase

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("heron_room.db")
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}