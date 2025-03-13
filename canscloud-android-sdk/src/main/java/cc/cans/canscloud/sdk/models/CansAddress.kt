package cc.cans.canscloud.sdk.models

import androidx.annotation.Keep

@Keep
data class CansAddress(
    var port: Int,
    var domain: String,
    var username: String,
    var password: String,
    var transport: CansTransport,
    var displayName: String
)
