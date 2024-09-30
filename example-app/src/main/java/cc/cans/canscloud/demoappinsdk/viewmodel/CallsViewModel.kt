package cc.cans.canscloud.demoappinsdk.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cans
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
                CallState.Connected ->  callDuration.value = cans.durationTime
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
        cans.addListener(listener)
        callDuration.value = cans.durationTime
    }

    fun setAudio() {
        if (cans.isBluetoothState) {
            isBluetooth.value = true
        } else {
            isBluetooth.value = false
            setSpeaker()
        }
    }

    fun setSpeaker() {
        if (cans.isSpeakerState) {
            isSpeaker.value = true
        } else {
            isSpeaker.value = false
        }
    }

    override fun onCleared() {
        cans.removeListener(listener)
        super.onCleared()
    }
}
