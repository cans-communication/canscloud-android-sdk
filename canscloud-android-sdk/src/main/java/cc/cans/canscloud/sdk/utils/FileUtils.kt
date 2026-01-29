package cc.cans.canscloud.sdk.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class FileUtils {
    companion object {
        private const val TAG = "FIX_BUG"

        fun getFileStorageDir(context: Context, isPicture: Boolean = true): File {
            val type = if (isPicture) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_DOWNLOADS
            val path = context.getExternalFilesDir(type)
                ?: File(context.filesDir, if (isPicture) "Pictures" else "Downloads")

            if (!path.exists()) {
                val created = path.mkdirs()
                Log.d(TAG, "[FileUtils] Directory created: $created at $path")
            }
            return path
        }

        fun getFileStoragePath(context: Context, fileName: String, isImage: Boolean = true): File {
            val path = getFileStorageDir(context, isImage)
            var file = File(path, fileName)
            var prefix = 1
            while (file.exists() && file.length() > 0) {
                val nameWithoutExt = fileName.substringBeforeLast(".")
                val ext = fileName.substringAfterLast(".", "")
                file = File(path, "${nameWithoutExt}_$prefix.$ext")
                prefix++
            }
            return file
        }

        fun getIncomingFilePath(context: Context, fileName: String): String {
            val storageDir = getFileStorageDir(context, true)
            val file = File(storageDir, fileName)
            return file.absolutePath
        }

        suspend fun getFilePath(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
            try {
                val name = getNameFromUri(uri, context)
                val destFile = getFileStoragePath(context, name, true)

                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    FileInputStream(pfd.fileDescriptor).use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                return@withContext destFile.absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "Error copying file: ${e.message}")
                null
            }
        }

        private fun getNameFromUri(uri: Uri, context: Context): String {
            var name = "IMG_${System.currentTimeMillis()}.jpg"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        val fileName = cursor.getString(nameIndex)
                        if (!fileName.isNullOrEmpty()) name = fileName
                    }
                }
            }
            return name
        }
    }
}