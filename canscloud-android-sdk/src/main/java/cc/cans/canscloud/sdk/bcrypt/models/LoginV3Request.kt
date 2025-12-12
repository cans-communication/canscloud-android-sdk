package cc.cans.canscloud.sdk.bcrypt.models

import com.google.gson.annotations.SerializedName

data class LoginV3Request(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)
