package com.example.dovashiapp.storage

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Foundation.writeToFile
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned

/** The same contract as `AndroidFileStorageTest`, on Foundation file APIs. */
@OptIn(ExperimentalForeignApi::class)
class IosFileStorageTest {
    private val files = NSFileManager.defaultManager
    private val base = NSTemporaryDirectory().trimEnd('/') + "/storage-" + NSUUID().UUIDString
    private val root = "$base/root"
    private val outside = "$base/outside.txt"
    private val storage: IosFileStorage

    init {
        files.createDirectoryAtPath(root, withIntermediateDirectories = true, attributes = null, error = null)
        write(outside, byteArrayOf(1))
        storage = IosFileStorage(root)
    }

    @AfterTest
    fun tearDown() {
        files.removeItemAtPath(base, error = null)
    }

    private fun write(path: String, bytes: ByteArray) {
        files.createDirectoryAtPath(path.substringBeforeLast('/'), withIntermediateDirectories = true, attributes = null, error = null)
        val data = if (bytes.isEmpty()) NSData() else bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
        data.writeToFile(path, atomically = true)
    }

    @Test
    fun existingRelativeFileResolves() {
        write("$root/audio/a.m4a", byteArrayOf(1))
        assertNotNull(storage.resolve("audio/a.m4a"))
        assertTrue(storage.resolve("audio/a.m4a")!!.endsWith("/root/audio/a.m4a"))
    }

    @Test
    fun missingBlankAbsoluteAndEscapingReferencesAreNull() {
        assertNull(storage.resolve("audio/missing.m4a"))
        assertNull(storage.resolve(" "))
        assertNull(storage.resolve(outside))
        assertNull(storage.resolve("../outside.txt"))
        assertNull(storage.resolve("audio/../../outside.txt"))
    }

    @Test
    fun symlinkPointingOutsideTheRootIsNull() {
        files.createDirectoryAtPath("$root/audio", withIntermediateDirectories = true, attributes = null, error = null)
        files.createSymbolicLinkAtPath("$root/audio/link.m4a", withDestinationPath = outside, error = null)
        assertNull(storage.resolve("audio/link.m4a"))
    }

    @Test
    fun readReturnsBytesOrNull() {
        write("$root/audio/a.m4a", byteArrayOf(7, 8))
        assertEquals(listOf<Byte>(7, 8), storage.read("audio/a.m4a")?.toList())
        assertEquals(2L, storage.size("audio/a.m4a"))
        assertNull(storage.size("audio/missing.m4a"))
        assertNull(storage.read("audio/missing.m4a"))
        assertNull(storage.read("../outside.txt"))
    }

    @Test
    fun newRecordingReferencesAreUniqueM4aFilesUnderAudio() {
        val a = storage.newRecordingReference()
        val b = storage.newRecordingReference()
        assertTrue(a.startsWith("audio/") && a.endsWith(".m4a"), a)
        assertNotEquals(a, b)
    }

    @Test
    fun writablePathCreatesTheFolderAndRejectsEscapes() {
        val path = storage.writablePath("audio/new.m4a")
        assertNotNull(path)
        assertTrue(path.endsWith("/root/audio/new.m4a"))
        assertTrue(files.fileExistsAtPath("$root/audio"))
        assertNull(storage.writablePath("../outside.txt"))
        assertNull(storage.writablePath("/abs.m4a"))
        assertNull(storage.writablePath(" "))
    }

    @Test
    fun deleteRemovesOnlyFilesInsideTheRoot() {
        write("$root/audio/a.m4a", byteArrayOf(1))
        storage.delete("audio/a.m4a")
        assertNull(storage.resolve("audio/a.m4a"))
        storage.delete("../outside.txt")
        assertTrue(files.fileExistsAtPath(outside))
    }

    @Test
    fun directoryIsNull() {
        files.createDirectoryAtPath("$root/audio", withIntermediateDirectories = true, attributes = null, error = null)
        assertNull(storage.resolve("audio"))
    }
}
