package cc.cans.canscloud.sdk.models

import androidx.annotation.Keep

@Keep
data class ConferenceModel(
    var address: String,
    var phoneNumber: String,
    var name: String,
    var duration: String,
)
