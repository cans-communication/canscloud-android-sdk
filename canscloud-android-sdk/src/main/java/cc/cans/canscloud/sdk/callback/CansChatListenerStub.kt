package cc.cans.canscloud.sdk.callback

interface CansChatListenerStub {
    fun onMessageReceived(sender: String, text: String, messageId: String)
}