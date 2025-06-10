package cc.cans.canscloud.sdk.models

import androidx.annotation.Keep

@Keep
data class CallModel(
    var callID: String,
    var address: String,
    var phoneNumber: String,
    var name: String,
    var isPaused: Boolean,
    var status: CallState,
    var duration: String,
)
