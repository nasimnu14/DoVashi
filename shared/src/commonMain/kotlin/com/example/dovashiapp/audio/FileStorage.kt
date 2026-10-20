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

    /** A fresh, unused reference for a new Recording, e.g. `audio/<uuid>.m4a`. */
    fun newRecordingReference(): String

    /**
     * Absolute path to write [reference] to (parent directories created), or null for a blank, absolute or escaping
     * reference. The file need not exist yet.
     */
    fun writablePath(reference: String): String?

    /** Deletes the file if it exists; never touches anything outside the storage root. */
    fun delete(reference: String)
}
