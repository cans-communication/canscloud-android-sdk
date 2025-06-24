package cc.cans.canscloud.demoappinsdk.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter
import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.models.ConferenceState
import cc.cans.canscloud.sdk.models.RegisterState

class CallsViewModel : ViewModel() {
    val callDuration = MutableLiveData<Int?>()
    var isCallEnd = MutableLiveData<Boolean>()
    var isBluetooth = MutableLiveData<Boolean>()
    var isSpeaker = MutableLiveData<Boolean>()

    private val listener = object : CansListenerStub {
        override fun onAudioDeviceChanged() {
            isSpeaker.value = cansCenter().isSpeakerState
            isBluetooth.value = cansCenter().isBluetoothState
        }

        override fun onAudioDevicesListUpdated() {}
        override fun onConferenceState(state: ConferenceState) {}

        override fun onCallState(
            state: CallState,
            message: String?
        ) {
            Log.i("[CallsViewModel] onCallState: ","$state")
            when (state) {
                CallState.CallOutgoing -> {}
                CallState.IncomingCall -> {}
                CallState.StartCall ->  {}
                CallState.Connected ->  callDuration.value = cansCenter().durationTime
                CallState.Error -> {}
                CallState.CallEnd -> {}
                CallState.MissCall -> {}
                CallState.Unknown -> {}
                CallState.Idle -> {}
                CallState.StreamsRunning -> {}
                CallState.Pause -> {}
                CallState.Resuming -> {}
            }
        }

        override fun onLastCallEnded() {
            isCallEnd.value = true
        }

        override fun onRegistration(state: RegisterState, message: String?) {
            Log.i("[CallsViewModel]","onRegistration ${state}")
        }

        override fun onUnRegister() {
        }
    }

    init {
        isSpeaker.value = cansCenter().isSpeakerState
        isBluetooth.value = cansCenter().isBluetoothState
        cansCenter().updateAudioRelated()

        cansCenter().addListener(listener)
        callDuration.value = cansCenter().durationTime
    }

    override fun onCleared() {
        cansCenter().removeListener(listener)
        super.onCleared()
    }
}
