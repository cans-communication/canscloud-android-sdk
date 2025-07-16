package cc.cans.canscloud.sdk.okta.models

import com.google.gson.annotations.SerializedName

data class OKTAClientResponseData(
    @SerializedName("code")
    val code: Int,
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: Data
) {
    data class Data(
        @SerializedName("okta_application_uuid")
        val oktaApplicationUuid: String,
        @SerializedName("domain_name")
        val domainName: String,
        @SerializedName("discovery_url")
        val discoveryUrl: String,
        @SerializedName("client_id")
        val clientId: String
    )
}


