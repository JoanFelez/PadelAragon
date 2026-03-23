package com.padelaragon.app

/**
 * Loads HTML fixture files from `test/resources/fixtures/` for deterministic
 * local parser tests. Fails loudly if a fixture is missing so broken test
 * setups surface immediately.
 */
object FixtureLoader {

    /**
     * Reads the entire content of `fixtures/<fileName>` from the test
     * resources directory. Throws [IllegalStateException] when the resource
     * cannot be found, which keeps test failures obvious.
     */
    fun load(fileName: String): String {
        val path = "fixtures/$fileName"
        val stream = javaClass.classLoader?.getResourceAsStream(path)
            ?: error("Fixture not found on classpath: $path")
        return stream.bufferedReader().use { it.readText() }
    }
}
