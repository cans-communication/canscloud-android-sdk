package cc.cans.canscloud.sdk.callback

interface RegisterListeners {
    fun onRegistrationOk()
    fun onRegistrationFail(message: String)
    fun onUnRegister()
}