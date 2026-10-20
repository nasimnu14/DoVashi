package com.example.dovashiapp.architecture

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Doc 16: the module structure and the rules that let Phase 2 add languages by adding metadata only. These read
 * the source tree, so a violation fails the build rather than waiting for a review to notice.
 */
class ArchitectureTest {
    // Host tests run with the module directory (`shared`) as the working directory.
    private val shared = File("..", "shared/src").canonicalFile
    private val base = "com/example/dovashiapp"

    private fun sourceFiles(vararg sourceSets: String): List<File> = sourceSets.flatMap { set ->
        File(shared, "$set/kotlin/$base").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private fun File.relative(): String = relativeTo(shared).path

    private fun File.imports(): List<String> = readLines().map { it.trim() }.filter { it.startsWith("import ") }.map { it.removePrefix("import ") }

    /** Code lines only: comments and KDoc may mention anything. */
    private fun File.codeLines(): List<String> = readLines().map { it.trim() }.filterNot { it.startsWith("//") || it.startsWith("*") || it.startsWith("/*") }

    private val main = arrayOf("commonMain", "androidMain", "iosMain")

    @Test
    fun moduleStructureMatchesTheSpec() {
        val common = listOf("data/network", "data/repository", "domain/model", "domain/repository", "domain/usecase",
            "presentation/home", "presentation/conversation", "presentation/createconversation", "audio", "navigation", "di")
        val platform = listOf("audio", "permissions", "storage")
        common.forEach { assertTrue(File(shared, "commonMain/kotlin/$base/$it").isDirectory, "commonMain/$it is missing") }
        listOf("androidMain", "iosMain").forEach { set ->
            platform.forEach { assertTrue(File(shared, "$set/kotlin/$base/$it").isDirectory, "$set/$it is missing") }
        }
        assertTrue(File(shared, "commonMain/sqldelight/$base/data/database").isDirectory, "the database schema folder is missing")
    }

    @Test
    fun domainDoesNotDependOnOuterLayers() {
        val offenders = sourceFiles("commonMain").filter { it.relative().contains("/domain/") }.filter { file ->
            file.imports().any { import -> listOf("data", "presentation", "di", "navigation").any { import.startsWith("com.example.dovashiapp.$it.") } }
        }.map { it.relative() }
        assertEquals(emptyList(), offenders, "domain must not import data, presentation, di or navigation")
    }

    @Test
    fun presentationDoesNotReachIntoData() {
        val offenders = sourceFiles("commonMain").filter { it.relative().contains("/presentation/") }
            .filter { file -> file.imports().any { it.startsWith("com.example.dovashiapp.data.") } }.map { it.relative() }
        assertEquals(emptyList(), offenders, "presentation talks to domain, never to data")
    }

    @Test
    fun commonCodeUsesNoPlatformApis() {
        val platformPrefixes = listOf("android.", "java.", "javax.", "androidx.room", "platform.")
        val offenders = sourceFiles("commonMain").filter { file -> file.imports().any { i -> platformPrefixes.any { i.startsWith(it) } } }.map { it.relative() }
        assertEquals(emptyList(), offenders, "commonMain must stay platform-free (use an interface and a platform implementation)")
    }

    @Test
    fun openAiIsReachableOnlyThroughItsPorts() {
        val adapters = Regex("""\bOpenAi(TranslationService|SpeechToTextService)\b""")
        val allowed = listOf("/data/network/openai/", "/di/")
        val offenders = sourceFiles(*main).filter { f -> allowed.none { f.relative().contains(it) } }
            .filter { f -> f.codeLines().any { adapters.containsMatchIn(it) } }.map { it.relative() }
        assertEquals(emptyList(), offenders, "only DI may name the OpenAI adapters; everything else uses the ports")
    }

    @Test
    fun noLanguageIsHardCodedOutsideTheCatalogMetadata() {
        val metadata = setOf("LanguageCatalog.kt", "WhisperLanguages.kt")
        val literal = Regex("\"(en|zh)\"")
        val files = sourceFiles(*main) + File(shared.parentFile.parentFile, "androidApp/src/main").walkTopDown().filter { it.isFile && it.extension == "kt" }
        val offenders = files.filter { it.name !in metadata }.filter { f -> f.codeLines().any { literal.containsMatchIn(it) } }.map { it.name }
        assertEquals(emptyList(), offenders, "language codes live in the Language Catalog (and Whisper's name table) only")
    }
}
