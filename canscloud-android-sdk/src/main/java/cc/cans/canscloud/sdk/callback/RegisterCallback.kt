package cc.cans.canscloud.sdk.callback

interface RegisterCallback {
    fun onRegistrationOk()
    fun onRegistrationFail(message: String)
    fun onUnRegister()
}