package cc.cans.canscloud.sdk.models

import androidx.annotation.Keep
import org.linphone.core.Address

@Keep
data class HistoryModel(
    var phoneNumber: String,
    var name: String,
    var state: CallState,
    var time: String,
    var date: String,
    var startDate: Long,
    var duration: String,
    var callID: String,
    var localAddress: Address,
    var remoteAddress: Address,
    var listCall: Int? = 0
)
