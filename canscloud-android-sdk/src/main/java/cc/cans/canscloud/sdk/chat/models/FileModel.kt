package cc.cans.canscloud.sdk.chat.models

import android.util.Log
import androidx.lifecycle.MutableLiveData
//import cc.cans.obsidianvoiceandroid.utils.FileUtils
import java.io.File

class FileModel(
    val fileName: String,
    initialPath: String?,
    private val onDownloadRequired: () -> Unit
) {
    val localPath = MutableLiveData<String?>()
    val transferProgress = MutableLiveData<Int>()
    val isDownloading = MutableLiveData<Boolean>()

    init {
        transferProgress.postValue(0)
        isDownloading.postValue(false)
//        checkLocalFile(initialPath)
    }

//    fun checkLocalFile(path: String?) {
//        val targetPath = path ?: FileUtils.getIncomingFilePath(fileName)
//        val file = File(targetPath)
//
//        Log.d("FIX_BUG", "[FileModel] Checking: ${file.absolutePath} | Exists: ${file.exists()} | Size: ${file.length()}")
//
//        if (file.exists() && file.length() > 0) {
//            Log.i("FIX_BUG", "[FileModel] VALID FILE FOUND. Update UI.")
//            localPath.postValue(file.absolutePath)
//            isDownloading.postValue(false)
//            transferProgress.postValue(100)
//        } else {
//            Log.w("FIX_BUG", "[FileModel] File invalid or missing. Requesting download.")
//            localPath.postValue(null)
//            onDownloadRequired()
//        }
//    }
}