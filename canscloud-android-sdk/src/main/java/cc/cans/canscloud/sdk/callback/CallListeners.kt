package cc.cans.canscloud.sdk.callback

import cc.cans.canscloud.sdk.models.CallState

interface CallListeners {
   fun onCallState(state : CallState, message: String = "")
}