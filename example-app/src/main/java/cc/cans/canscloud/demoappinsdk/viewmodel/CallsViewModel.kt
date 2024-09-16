package cc.cans.canscloud.demoappinsdk.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.sdk.models.AudioState
import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.models.RegisterState

class CallsViewModel : ViewModel() {
    val callDuration = MutableLiveData<Int?>()
    var isCallEnd = MutableLiveData<Boolean>()
    var isBluetooth = MutableLiveData<Boolean>()

    private val listener = object : CansListenerStub {
        override fun onRegistration(state: RegisterState, message: String?) {
            Log.i("[CallsViewModel]","onRegistration ${state}")
        }

        override fun onUnRegister() {
        }

        override fun onCallState(state: CallState, message: String?) {
            Log.i("[CallsViewModel] onCallState: ","$state")
            when (state) {
                CallState.IncomingCall -> {}
                CallState.StartCall ->  {}
                CallState.CallOutgoing -> {}
                CallState.StreamsRunning -> {}
                CallState.Connected ->  callDuration.value = Cans.durationTime
                CallState.Error -> {}
                CallState.CallEnd -> {}
                CallState.LastCallEnd ->  isCallEnd.value = true
                CallState.MissCall -> {}
                CallState.Unknown -> {}
            }
        }

        override fun onAudioUpdate(state: AudioState) {
            Log.i("[CallsViewModel onAudioUpdate]", "Audio devices $state")
            isBluetooth.value = state == AudioState.Bluetooth
        }
    }

    init {
        Cans.addListener(listener)
        callDuration.value = Cans.durationTime
    }

    override fun onCleared() {
        Cans.removeListener(listener)
        super.onCleared()
    }
}
