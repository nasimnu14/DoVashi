package com.example.dovashiapp.storage

import com.example.dovashiapp.audio.FileStorage
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

/** [FileStorage] under [rootPath] (Foundation file APIs). References are relative paths such as `audio/<uuid>.m4a`. */
@OptIn(ExperimentalForeignApi::class)
class IosFileStorage(rootPath: String) : FileStorage {
    private val files = NSFileManager.defaultManager
    private val root: String = canonical(rootPath)

    override fun resolve(reference: String): String? = pathUnderRoot(reference)?.takeIf { isFile(it) }

    override fun size(reference: String): Long? = resolve(reference)?.let { path ->
        (files.attributesOfItemAtPath(path, error = null)?.get("NSFileSize") as? Number)?.toLong()
    }

    override fun read(reference: String): ByteArray? {
        val path = resolve(reference) ?: return null
        val data = NSData.dataWithContentsOfFile(path) ?: return null
        return ByteArray(data.length.toInt()).also { bytes ->
            if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
        }
    }

    override fun newRecordingReference(): String = "audio/${NSUUID().UUIDString.lowercase()}.m4a"

    override fun writablePath(reference: String): String? {
        val path = pathUnderRoot(reference) ?: return null
        if (isDirectory(path)) return null
        val parent = path.substringBeforeLast('/')
        files.createDirectoryAtPath(parent, withIntermediateDirectories = true, attributes = null, error = null)
        return path
    }

    override fun delete(reference: String) {
        val path = pathUnderRoot(reference)?.takeIf { isFile(it) } ?: return
        files.removeItemAtPath(path, error = null)
    }

    /** The standardized path for [reference] if it lies strictly inside the root; null otherwise. */
    private fun pathUnderRoot(reference: String): String? {
        if (reference.isBlank() || reference.startsWith("/") || '\u0000' in reference) return null
        val path = canonical("$root/$reference")
        return path.takeIf { it.startsWith("$root/") }
    }

    private fun isFile(path: String) = exists(path) && !isDirectory(path)

    private fun exists(path: String) = files.fileExistsAtPath(path)

    private fun isDirectory(path: String): Boolean = memScoped {
        val directory = alloc<BooleanVar>()
        files.fileExistsAtPath(path, isDirectory = directory.ptr) && directory.value
    }

    private companion object {
        /** Symlinks resolved and `..` removed, so `/var` and `/private/var` compare equal. */
        fun canonical(path: String): String = NSURL.fileURLWithPath(path).URLByResolvingSymlinksInPath!!.URLByStandardizingPath!!.path!!
    }
}
