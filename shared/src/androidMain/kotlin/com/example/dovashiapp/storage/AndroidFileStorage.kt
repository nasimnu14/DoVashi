package com.example.dovashiapp.storage

import com.example.dovashiapp.audio.FileStorage
import java.io.File
import java.io.IOException

class AndroidFileStorage(rootDir: File) : FileStorage {
    private val root: File = rootDir.canonicalFile

    override fun resolve(reference: String): String? {
        if (reference.isBlank() || File(reference).isAbsolute) return null
        val file = try {
            File(root, reference).canonicalFile
        } catch (e: IOException) {
            return null // e.g. a NUL character or a symlink loop in a malformed stored reference
        }
        if (!file.path.startsWith(root.path + File.separator)) return null
        return file.takeIf { it.isFile }?.path
    }
}
