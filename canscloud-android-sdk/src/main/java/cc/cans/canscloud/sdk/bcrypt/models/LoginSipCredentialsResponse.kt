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

/** The SIP identity an account is created with: what lookup and sign-out later know it by. */
data class SipAccountIdentity(val username: String, val domain: String) {
    /** `<username>@<domain>`, the key per-account values are stored and read under. */
    val address: String get() = "$username@$domain"
}

/**
 * Resolves the account's identity from `sip-credentials`, falling back to what the user signed in
 * with: [loginUsername] when the response has no extension, [loginDomain] when it has no
 * `domain_name`. A port on the domain is dropped.
 */
fun LoginSipCredentialsData.sipAccountIdentity(
    loginUsername: String,
    loginDomain: String,
): SipAccountIdentity = SipAccountIdentity(
    username = extension ?: loginUsername,
    domain = (domainName ?: loginDomain).substringBefore(':'),
)
