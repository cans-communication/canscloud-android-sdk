package cc.cans.canscloud.sdk.bcrypt.models

import com.google.gson.annotations.SerializedName

data class  LoginSipCredentialsResponse(
    @SerializedName("data") val data: LoginSipCredentialsData?,
    @SerializedName("message") val message: String?,
    @SerializedName("code") val code: Int? = null,
)

data class  LoginSipCredentialsData(
    @SerializedName("extension") val extension: String?,
    @SerializedName("domain_name") val domainName: String?,
    @SerializedName("sip_creds") val sipCreds: String?
)