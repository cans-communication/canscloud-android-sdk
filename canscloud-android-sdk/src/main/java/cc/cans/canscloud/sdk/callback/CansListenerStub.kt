package cc.cans.canscloud.sdk.callback

import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.models.RegisterState

interface CansListenerStub {
   fun onRegistration(state : RegisterState, message: String? = null)
   fun onUnRegister()
   fun onCallState(state : CallState, message: String? = null)
   fun onLastCallEnded()
   fun onAudioDeviceChanged()
   fun onAudioDevicesListUpdated()
}