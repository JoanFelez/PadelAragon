package com.padelaragon.shared.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

/** Per-user app data directory (default `~/.padelaragon`). */
actual class PlatformContext(
    val appDataDir: File = File(System.getProperty("user.home"), ".padelaragon")
)

actual fun appDatabaseBuilder(context: PlatformContext): RoomDatabase.Builder<AppDatabase> {
    context.appDataDir.mkdirs()
    return Room.databaseBuilder<AppDatabase>(
        name = File(context.appDataDir, "padel_aragon.db").absolutePath
    )
}
