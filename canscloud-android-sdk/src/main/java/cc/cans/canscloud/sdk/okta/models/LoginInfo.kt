package cc.cans.canscloud.sdk.okta.models

data class LoginInfo(
    var logInType: String? = "",
    var tokenSignIn: String? = "",
    var domainOKTACurrent: String? = "",
    var tokenOkta: String? = ""
)
