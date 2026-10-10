package com.example.dovashiapp.audio

/** App-private file storage. References are relative paths under its root, e.g. `audio/<name>.m4a`. */
interface FileStorage {
    /**
     * Absolute path of an existing file for [reference], or null when the reference is blank, absolute,
     * escapes the storage root, or names no file.
     */
    fun resolve(reference: String): String?

    /** Size in bytes, or null under the same conditions as [resolve]. */
    fun size(reference: String): Long?

    /** The file's bytes, or null under the same conditions as [resolve] or when it can't be read. */
    fun read(reference: String): ByteArray?
}
