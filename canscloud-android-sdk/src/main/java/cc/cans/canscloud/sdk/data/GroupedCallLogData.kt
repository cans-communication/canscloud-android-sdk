package cc.cans.canscloud.sdk.data

import cc.cans.canscloud.sdk.models.HistoryModel
import org.linphone.core.CallLog

class GroupedCallLogData(callLog: HistoryModel) {
    var lastCallLog: HistoryModel = callLog
    val callLogs = arrayListOf(callLog)
   // val lastCallLogData = CallLogData(lastCallLog)

    fun destroy() {
     //   lastCallLogData.destroy()
    }
}