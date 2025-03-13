package cc.cans.canscloud.sdk.models

import androidx.annotation.Keep

@Keep
data class HistoryModel(
    var phoneNumber: String,
    var name: String,
    var state: CallState,
    var time: String,
    var date: String,
    var startDate: Long,
    var duration: String,
    var localAddress: CansAddress,
    var remoteAddress: CansAddress
)
