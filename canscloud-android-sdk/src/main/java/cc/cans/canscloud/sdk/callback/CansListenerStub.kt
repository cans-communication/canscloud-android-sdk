package cc.cans.canscloud.sdk.callback

import cc.cans.canscloud.sdk.models.AudioState
import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.models.RegisterState
import org.linphone.core.Call

interface CansListenerStub {
   fun onRegistration(state : RegisterState, message: String? = null)
   fun onUnRegister()
   fun onCallState(call: Call, state : CallState, message: String? = null)
   fun onLastCallEnded()
   fun onAudioDeviceChanged()
   fun onAudioDevicesListUpdated()
}