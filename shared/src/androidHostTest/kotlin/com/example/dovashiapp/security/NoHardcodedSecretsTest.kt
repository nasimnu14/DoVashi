package com.example.dovashiapp.security

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Doc 15: the OpenAI key is never hard-coded or committed. */
class NoHardcodedSecretsTest {
    // Host tests run with the module directory (`shared`) as the working directory.
    private val repoRoot = File("..").canonicalFile
    private val keyShape = Regex("sk-[A-Za-z0-9_-]{20,}")
    private val textExtensions = setOf("kt", "kts", "swift", "plist", "xcconfig", "xml", "sq", "sqm", "toml", "properties", "md", "json")

    @Test
    fun noSourceFileContainsAnApiKey() {
        val roots = listOf("shared/src", "androidApp/src", "iosApp").map { File(repoRoot, it) }
        assertTrue(roots.all { it.isDirectory }, "scan roots missing under $repoRoot")
        val offenders = roots.flatMap { root ->
            root.walkTopDown()
                .onEnter { it.name != "build" && it.name != "xcuserdata" }
                .filter { it.isFile && it.extension in textExtensions }
                .filter { keyShape.containsMatchIn(it.readText()) }
                .map { it.relativeTo(repoRoot).path }
                .toList()
        }
        assertEquals(emptyList(), offenders, "files that look like they contain an OpenAI key")
    }

    @Test
    fun keyFilesAreGitIgnored() {
        val ignored = File(repoRoot, ".gitignore").readLines().map { it.trim() }
        listOf("local.properties", "*.key", "Secrets.xcconfig").forEach { assertTrue(it in ignored, "$it must be in .gitignore") }
    }
}
