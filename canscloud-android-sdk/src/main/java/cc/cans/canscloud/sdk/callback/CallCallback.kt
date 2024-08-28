package cc.cans.canscloud.sdk.callback

interface CallCallback {
    fun onStartCall()
    fun onConnected()
    fun onError(message: String)
}