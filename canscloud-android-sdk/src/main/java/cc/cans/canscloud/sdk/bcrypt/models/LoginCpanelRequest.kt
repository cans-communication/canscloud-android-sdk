package cc.cans.canscloud.sdk.bcrypt.models

import com.google.gson.annotations.SerializedName

data class LoginCpanelRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("login_type") val loginType: String
)
