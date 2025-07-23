package cc.cans.canscloud.sdk.okta.models

import com.google.gson.annotations.SerializedName

data class SignIn(
    @SerializedName("sip_username")
    val sip_username: String? = null,
    @SerializedName("sip_password")
    val sip_password: String? = null
)