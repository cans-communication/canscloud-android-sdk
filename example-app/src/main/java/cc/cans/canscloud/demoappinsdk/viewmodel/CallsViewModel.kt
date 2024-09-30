package cc.cans.canscloud.demoappinsdk.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter
import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.models.RegisterState

class CallsViewModel : ViewModel() {
    val callDuration = MutableLiveData<Int?>()
    var isCallEnd = MutableLiveData<Boolean>()
    var isBluetooth = MutableLiveData<Boolean>()
    var isSpeaker = MutableLiveData<Boolean>()

    private val listener = object : CansListenerStub {
        override fun onAudioDeviceChanged() {
            setAudio()
        }

        override fun onAudioDevicesListUpdated() {
            setAudio()
        }

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
        cansCenter().addListener(listener)
        callDuration.value = cansCenter().durationTime
    }

    fun setAudio() {
        if (cansCenter().isBluetoothState) {
            isBluetooth.value = true
        } else {
            isBluetooth.value = false
            setSpeaker()
        }
    }

    fun setSpeaker() {
        if (cansCenter().isSpeakerState) {
            isSpeaker.value = true
        } else {
            isSpeaker.value = false
        }
    }

    override fun onCleared() {
        cansCenter().removeListener(listener)
        super.onCleared()
    }
}
