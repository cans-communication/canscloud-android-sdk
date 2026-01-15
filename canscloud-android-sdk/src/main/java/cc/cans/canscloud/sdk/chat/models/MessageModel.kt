package cc.cans.canscloud.sdk.chat.models

import android.util.Log
import androidx.lifecycle.MutableLiveData
//import cc.cans.obsidianvoiceandroid.LinphoneApplication.Companion.coreContext
//import cc.cans.obsidianvoiceandroid.utils.FileUtils
import org.linphone.core.*
import android.os.Handler
import android.os.Looper
import java.io.File

class MessageModel(
    val chatMessage: ChatMessage,
    val manualPath: String? = null
) {
    val id: String = chatMessage.messageId ?: "local_${chatMessage.hashCode()}"
    val isOutgoing: Boolean = chatMessage.isOutgoing
    val status = MutableLiveData<ChatMessage.State>()
    val fileModels = arrayListOf<FileModel>()

    private val handler = Handler(Looper.getMainLooper())

    private val listener = object : ChatMessageListenerStub() {
        override fun onMsgStateChanged(message: ChatMessage, state: ChatMessage.State) {
            status.postValue(state)
            Log.i("FIX_BUG", "[MessageModel] ID: $id State changed to: $state")

            if (state == ChatMessage.State.FileTransferDone ||
                state == ChatMessage.State.Delivered ||
                state == ChatMessage.State.Displayed) {
                handler.postDelayed({
//                    computeImagePath()
                }, 500)
            }
        }

        override fun onFileTransferProgressIndication(message: ChatMessage, content: Content, offset: Int, total: Int) {
            val percent = if (total > 0) (offset * 100 / total) else 0
            Log.d("FIX_BUG", "[MessageModel] ID: $id Downloading: $percent%")
            fileModels.firstOrNull()?.let {
                it.isDownloading.postValue(percent < 100)
                it.transferProgress.postValue(percent)

                if (percent == 100) {
                    Log.i("FIX_BUG", "[MessageModel] Download 100% reached, checking file in 500ms...")
                    handler.postDelayed({
//                        computeImagePath()
                    }, 500)
                }
            }
        }
    }

    init {
        chatMessage.addListener(listener)
        status.postValue(chatMessage.state)

        val customFileName = chatMessage.getCustomHeader("X-Local-Filename")
        Log.d("FIX_BUG", "[MessageModel] Init ID: $id | HeaderName: $customFileName | Contents: ${chatMessage.contents.size}")

        chatMessage.contents.forEachIndexed { index, content ->
            val isProbablyFile = content.isFileTransfer ||
                    content.type?.contains("image") == true ||
                    !customFileName.isNullOrEmpty()

            Log.d("FIX_BUG", "[MessageModel] Content[$index] type: ${content.type} | isFile: ${content.isFileTransfer}")

//            if (isProbablyFile) {
//                val fileName = content.name ?: customFileName ?: "img.jpg"
//
//                val fModel = FileModel(fileName, manualPath ?: content.filePath) {
//                    if (!isOutgoing) {
//                        val currentState = status.value
//                        if (currentState == ChatMessage.State.FileTransferInProgress ||
//                            currentState == ChatMessage.State.InProgress) {
//                            Log.w("FIX_BUG", "[MessageModel] Download requested but already in progress. Ignoring.")
//                            return@FileModel
//                        }
//
//                        val destinationPath = FileUtils.getIncomingFilePath(fileName)
//                        val destFile = File(destinationPath)
//
//                        try {
//                            if (destFile.parentFile?.exists() == false) {
//                                val created = destFile.parentFile?.mkdirs()
//                                Log.i("FIX_BUG", "[MessageModel] Created missing directory: $created")
//                            }
//                        } catch (e: Exception) {
//                            Log.e("FIX_BUG", "[MessageModel] Failed to create directory", e)
//                        }
//
//                        // ลบไฟล์ขยะ
//                        if (destFile.exists() && destFile.length() == 0L) {
//                            destFile.delete()
//                        }
//
//                        // กำหนด Path และสั่งโหลด
//                        content.filePath = destinationPath
//                        Log.i("FIX_BUG", "[MessageModel] Triggering Download for $fileName to $destinationPath")
//                        chatMessage.downloadContent(content)
//                    }
//                }
//                fileModels.add(fModel)
//            }
        }
    }

//    fun computeImagePath() {
//        Log.d("FIX_BUG", "[MessageModel] computeImagePath called for ID: $id")
//        fileModels.forEach { it.checkLocalFile(it.localPath.value) }
//    }

    fun destroy() {
        chatMessage.removeListener(listener)
    }
}