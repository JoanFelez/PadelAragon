package com.padelaragon.shared.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual class PlatformContext(val context: Context)

actual fun appDatabaseBuilder(context: PlatformContext): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.context.applicationContext
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = appContext.getDatabasePath(DATABASE_NAME).absolutePath
    )
}

private const val DATABASE_NAME = "padel_aragon.db"
