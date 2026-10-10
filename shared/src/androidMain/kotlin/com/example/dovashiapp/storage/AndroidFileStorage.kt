package com.example.dovashiapp.storage

import com.example.dovashiapp.audio.FileStorage
import java.io.File
import java.io.IOException
import java.util.UUID

class AndroidFileStorage(rootDir: File) : FileStorage {
    private val root: File = rootDir.canonicalFile

    override fun resolve(reference: String): String? = fileUnderRoot(reference)?.takeIf { it.isFile }?.path

    override fun newRecordingReference(): String = "audio/${UUID.randomUUID()}.m4a"

    override fun writablePath(reference: String): String? {
        val file = fileUnderRoot(reference) ?: return null
        if (file.isDirectory) return null
        file.parentFile?.mkdirs()
        return file.path
    }

    override fun delete(reference: String) {
        fileUnderRoot(reference)?.takeIf { it.isFile }?.delete()
    }

    /** The canonical file for [reference] if it lies strictly inside the root; null otherwise. */
    private fun fileUnderRoot(reference: String): File? {
        if (reference.isBlank() || File(reference).isAbsolute) return null
        val file = try {
            File(root, reference).canonicalFile
        } catch (e: IOException) {
            return null // e.g. a NUL character or a symlink loop in a malformed stored reference
        }
        return file.takeIf { it.path.startsWith(root.path + File.separator) }
    }

    override fun size(reference: String): Long? = resolve(reference)?.let { File(it).length() }

    override fun read(reference: String): ByteArray? {
        val path = resolve(reference) ?: return null
        return try {
            File(path).readBytes()
        } catch (e: IOException) {
            null
        }
    }
}
