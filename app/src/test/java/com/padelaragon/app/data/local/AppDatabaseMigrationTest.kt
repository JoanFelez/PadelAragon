package com.padelaragon.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDatabaseMigrationTest {
    @Test
    fun `migrates every version three cache table into the default league namespace`() {
        val statements = mutableListOf<String>()
        val database = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, arguments ->
            if (method.name == "execSQL") statements += arguments?.first() as String
            defaultValue(method.returnType)
        } as SupportSQLiteDatabase

        AppDatabase.MIGRATION_3_4.migrate(database)

        val tables = listOf(
            "league_groups", "standings", "match_results", "match_detail_pairs",
            "team_details", "players", "jornadas"
        )
        tables.forEach { table ->
            assertTrue("Expected $table to be renamed", "ALTER TABLE $table RENAME TO ${table}_v3" in statements)
            assertTrue(
                "Expected $table data to retain its default league identity",
                "INSERT INTO $table SELECT 27951, * FROM ${table}_v3" in statements
            )
            assertTrue("Expected $table temporary data to be removed", "DROP TABLE ${table}_v3" in statements)
        }
        assertEquals(28, statements.size)
    }

    private fun defaultValue(returnType: Class<*>): Any? = when (returnType) {
        Boolean::class.javaPrimitiveType -> false
        Int::class.javaPrimitiveType -> 0
        Long::class.javaPrimitiveType -> 0L
        Float::class.javaPrimitiveType -> 0f
        Double::class.javaPrimitiveType -> 0.0
        else -> null
    }
}
