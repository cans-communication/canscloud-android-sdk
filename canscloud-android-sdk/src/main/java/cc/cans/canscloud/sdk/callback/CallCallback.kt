package cc.cans.canscloud.sdk.callback

interface CallCallback {
    fun onCallOutGoing()
    fun onLastCallEnd()
    fun onInComingCall()
    fun onStartCall()
    fun onConnected()
    fun onError(message: String)
    fun onCallEnd()
    fun onCall()
}