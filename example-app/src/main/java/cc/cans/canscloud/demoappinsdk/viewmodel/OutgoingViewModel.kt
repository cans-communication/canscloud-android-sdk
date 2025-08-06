package cc.cans.canscloud.demoappinsdk.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter
import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.models.ConferenceState
import cc.cans.canscloud.sdk.models.RegisterState
class OutgoingViewModel : ViewModel() {

    var isCallEnd = MutableLiveData<Boolean>()

    private val listener = object : CansListenerStub {
        override fun onCallState(state: CallState, message: String?) {
            Log.i("[OutgoingViewModel] onCallState: ", "$state")
            when (state) {
                CallState.Idle -> {}
                CallState.IncomingCall -> {}
                CallState.StartCall -> {}
                CallState.CallOutgoing -> {}
                CallState.StreamsRunning -> {}
                CallState.Connected -> {}
                CallState.Error -> {}
                CallState.CallEnd -> {}
                CallState.MissCall -> {}
                CallState.Pause -> {}
                CallState.Resuming -> {}
                CallState.Unknown -> {}
            }
        }

        override fun onLastCallEnded() {
            Log.i("[OutgoingViewModel]", "onLastCallEnded")
            isCallEnd.value = true
        }

        override fun onAudioDeviceChanged() {
            Log.i("[OutgoingViewModel onAudioUpdate]", "onAudioDeviceChanged")
        }

        override fun onAudioDevicesListUpdated() {
            Log.i("[OutgoingViewModel onAudioUpdate]", "onAudioDevicesListUpdated")
        }

        override fun onConferenceState(state: ConferenceState) {
        }
    }

    init {
        cansCenter().isSpeakerState
        cansCenter().addCansCallListener(listener)
    }

    override fun onCleared() {
        cansCenter().removeCansCallListener(listener)
        super.onCleared()
    }
}
