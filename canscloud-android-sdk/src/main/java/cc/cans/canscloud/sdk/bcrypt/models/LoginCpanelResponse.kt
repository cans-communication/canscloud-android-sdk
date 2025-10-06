package cc.cans.canscloud.sdk.bcrypt.models

import com.google.gson.annotations.SerializedName

data class LoginCpanelResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("data") val data: TokenData?,
    @SerializedName("message") val message: String?,
    @SerializedName("success") val success: Boolean
)

data class TokenData(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("expire_at") val expireAt: Long
)
