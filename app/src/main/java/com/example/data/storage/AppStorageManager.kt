package com.example.data.storage

import android.content.Context
import java.io.File
import java.util.Locale

data class StorageUsageSummary(
    val profilePhotoBytes: Long = 0L,
    val preferencesBytes: Long = 0L,
    val cacheBytes: Long = 0L,
    val isClearingCache: Boolean = false,
) {
    val totalDataBytes: Long
        get() = profilePhotoBytes + preferencesBytes + cacheBytes
}

/**
 * Zero-permission inspector and cleaner strictly scoped to TrackPulse's app-private directories.
 */
object AppStorageManager {

    fun inspectStorage(context: Context): StorageUsageSummary {
        val currentAvatar = File(context.filesDir, ProfileImageStorage.AVATAR_FILE_NAME)
        val legacyAvatar = File(context.filesDir, ProfileImageStorage.LEGACY_AVATAR_FILE_NAME)
        val avatarBytes = safeFileLength(currentAvatar) + safeFileLength(legacyAvatar)

        val datastoreDir = File(context.filesDir, "datastore")
        val sharedPrefsDir = File(context.filesDir.parentFile, "shared_prefs")
        val prefsBytes = directorySize(datastoreDir) + directorySize(sharedPrefsDir)

        val internalCacheBytes = directorySize(context.cacheDir)
        val externalCacheBytes = context.externalCacheDir?.let { directorySize(it) } ?: 0L

        return StorageUsageSummary(
            profilePhotoBytes = avatarBytes,
            preferencesBytes = prefsBytes,
            cacheBytes = internalCacheBytes + externalCacheBytes,
        )
    }

    /**
     * Clears app-private cache files (including any legacy image cache directories) without
     * touching active session credentials, preferences, or the operator profile photo.
     *
     * @return Number of bytes freed.
     */
    fun clearCache(context: Context): Long {
        val before = inspectStorage(context).cacheBytes
        deleteDirectoryChildren(context.cacheDir)
        context.externalCacheDir?.let { deleteDirectoryChildren(it) }
        val after = inspectStorage(context).cacheBytes
        return (before - after).coerceAtLeast(0L)
    }

    fun formatByteSize(bytes: Long): String {
        val safeBytes = bytes.coerceAtLeast(0L)
        return when {
            safeBytes < 1024L -> "$safeBytes B"
            safeBytes < 1024L * 1024L -> {
                val kb = safeBytes / 1024.0
                if (kb >= 100.0) {
                    String.format(Locale.US, "%.0f KB", kb)
                } else {
                    String.format(Locale.US, "%.1f KB", kb)
                }
            }
            else -> {
                val mb = safeBytes / (1024.0 * 1024.0)
                String.format(Locale.US, "%.2f MB", mb)
            }
        }
    }

    private fun safeFileLength(file: File): Long =
        if (file.exists() && file.isFile) file.length().coerceAtLeast(0L) else 0L

    private fun directorySize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        if (dir.isFile) return dir.length().coerceAtLeast(0L)
        var total = 0L
        val children = dir.listFiles() ?: return 0L
        for (child in children) {
            total += if (child.isDirectory) directorySize(child) else child.length().coerceAtLeast(0L)
        }
        return total
    }

    private fun deleteDirectoryChildren(dir: File?) {
        if (dir == null || !dir.exists() || !dir.isDirectory) return
        val children = dir.listFiles() ?: return
        for (child in children) {
            runCatching {
                if (child.isDirectory) {
                    child.deleteRecursively()
                } else {
                    child.delete()
                }
            }
        }
    }
}
