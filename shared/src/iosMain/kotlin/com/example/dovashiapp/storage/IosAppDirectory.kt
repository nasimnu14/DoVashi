package com.example.dovashiapp.storage

import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask

/**
 * The app-private directory holding the database and Recordings: `Library/Application Support/Bilangual`, created on
 * first use and excluded from iCloud/iTunes backups (what people said stays on the device, as on Android).
 */
@OptIn(ExperimentalForeignApi::class)
object IosAppDirectory {
    val path: String by lazy {
        val support = NSFileManager.defaultManager.URLsForDirectory(NSApplicationSupportDirectory, NSUserDomainMask).first() as NSURL
        val directory = support.URLByAppendingPathComponent("Bilangual", isDirectory = true)!!
        NSFileManager.defaultManager.createDirectoryAtURL(directory, withIntermediateDirectories = true, attributes = null, error = null)
        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            directory.setResourceValue(NSNumber(bool = true), forKey = NSURLIsExcludedFromBackupKey, error = error.ptr)
        }
        directory.path!!
    }
}
