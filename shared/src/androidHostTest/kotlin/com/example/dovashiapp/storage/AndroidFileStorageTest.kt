package com.example.dovashiapp.storage

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AndroidFileStorageTest {
    private val root: File = Files.createTempDirectory("storage").toFile()
    private val outside: File = File(root.parentFile, "outside-${root.name}.txt").apply { writeText("x") }
    private val storage = AndroidFileStorage(root)

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
        outside.delete()
    }

    @Test
    fun existingRelativeFileResolves() {
        val file = File(root, "audio/a.m4a").apply { parentFile.mkdirs(); writeText("a") }
        assertEquals(file.canonicalPath, storage.resolve("audio/a.m4a"))
    }

    @Test
    fun missingFileIsNull() = assertNull(storage.resolve("audio/missing.m4a"))

    @Test
    fun absoluteReferenceIsNull() = assertNull(storage.resolve(outside.absolutePath))

    @Test
    fun escapingReferenceIsNull() = assertNull(storage.resolve("../${outside.name}"))

    @Test
    fun blankReferenceIsNull() = assertNull(storage.resolve(" "))

    @Test
    fun symlinkPointingOutsideTheRootIsNull() {
        val link = File(root, "audio/link.m4a").apply { parentFile.mkdirs() }
        Files.createSymbolicLink(link.toPath(), outside.toPath())
        assertNull(storage.resolve("audio/link.m4a"))
    }

    @Test
    fun readReturnsBytesOrNull() {
        File(root, "audio/a.m4a").apply { parentFile.mkdirs(); writeBytes(byteArrayOf(7, 8)) }
        assertEquals(listOf<Byte>(7, 8), storage.read("audio/a.m4a")?.toList())
        assertEquals(2L, storage.size("audio/a.m4a"))
        assertNull(storage.size("audio/missing.m4a"))
        assertNull(storage.read("audio/missing.m4a"))
        assertNull(storage.read("../${outside.name}"))
    }

    @Test
    fun directoryIsNull() {
        File(root, "audio").mkdirs()
        assertNull(storage.resolve("audio"))
    }
}
