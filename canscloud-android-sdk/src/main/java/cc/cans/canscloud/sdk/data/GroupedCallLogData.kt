package cc.cans.canscloud.sdk.data

import cc.cans.canscloud.sdk.models.HistoryModel

class GroupedCallLogData(callLog: HistoryModel) {
    var lastCallLog: HistoryModel = callLog
    val callLogs = arrayListOf(callLog)
}