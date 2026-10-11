package com.example.dovashiapp

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Doc 13: the shared module is built on the mandated stack, and Room stays out of it. */
class TechStackTest {
    // Host tests run with the module directory (`shared`) as the working directory.
    private val repoRoot = File("..").canonicalFile
    private val sharedBuild = File(repoRoot, "shared/build.gradle.kts").readText()
    private val catalog = File(repoRoot, "gradle/libs.versions.toml").readText()

    @Test
    fun sharedModuleUsesTheMandatedStack() {
        listOf(
            "libs.plugins.kotlinMultiplatform", // Kotlin Multiplatform
            "libs.plugins.composeMultiplatform", // Compose Multiplatform
            "libs.plugins.sqldelight", // SQLDelight
            "libs.plugins.kotlinSerialization", // Kotlin Serialization
            "libs.ktor.client.core", // Ktor Client
            "libs.koin.core", // Koin
            "libs.kotlinx.coroutines.core", // Coroutines (+ Flow)
            "libs.kotlinx.serialization.json",
            "libs.sqldelight.runtime",
            "libs.compose.material3",
        ).forEach { assertTrue(it in sharedBuild, "shared/build.gradle.kts must use $it") }
    }

    @Test
    fun everyPlatformHasItsNativeDriverAndEngine() {
        listOf("libs.sqldelight.androidDriver", "libs.sqldelight.nativeDriver", "libs.ktor.client.okhttp", "libs.ktor.client.darwin")
            .forEach { assertTrue(it in sharedBuild, "shared/build.gradle.kts must use $it") }
    }

    @Test
    fun roomIsNotUsed() {
        assertFalse(catalog.contains("room", ignoreCase = true), "version catalog must not declare Room")
        assertFalse(sharedBuild.contains("room", ignoreCase = true), "shared build must not use Room")
    }
}
