package com.example.dovashiapp.audio

/** App-private file storage. References are relative paths under its root, e.g. `audio/<name>.m4a`. */
interface FileStorage {
    /**
     * Absolute path of an existing file for [reference], or null when the reference is blank, absolute,
     * escapes the storage root, or names no file.
     */
    fun resolve(reference: String): String?
}
