package cc.cans.canscloud.sdk.okta.models

import com.google.gson.annotations.SerializedName

data class SignInOKTAResponseData(
    @SerializedName("code")
    val code: Int,
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: Data
){
    data class Data(
        @SerializedName("access_token")
        var access_token: String,
        @SerializedName("user_credentials")
        var user_credentials: String,
        @SerializedName("domain_name")
        var domain_name: String,
        @SerializedName("port")
        var port: String,
        @SerializedName("transport")
        var transport: String,
        @SerializedName("expire_at")
        var expire_at: Int
    )
}
