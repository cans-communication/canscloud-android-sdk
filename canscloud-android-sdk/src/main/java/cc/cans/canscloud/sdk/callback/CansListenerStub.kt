package cc.cans.canscloud.sdk.callback

import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.models.ConferenceState
import cc.cans.canscloud.sdk.models.RegisterState

interface CansListenerStub {
   fun onCallState(state : CallState, message: String? = null)
   fun onLastCallEnded()
   fun onAudioDeviceChanged()
   fun onAudioDevicesListUpdated()
   fun onConferenceState(state : ConferenceState)
}