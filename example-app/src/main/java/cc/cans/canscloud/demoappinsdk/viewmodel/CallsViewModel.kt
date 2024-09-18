package cc.cans.canscloud.demoappinsdk.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cc.cans.canscloud.demoappinsdk.utils.AudioRouteUtils
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.sdk.Cans.Companion.isHeadsetAudioRouteAvailable
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.models.RegisterState
import org.linphone.core.Call

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

        override fun onCallState(call: Call, state: CallState, message: String?) {
            Log.i("[CallsViewModel] onCallState: ","$state")
            when (state) {
                CallState.Idle -> {}
                CallState.IncomingCall -> {}
                CallState.StartCall ->  {}
                CallState.CallOutgoing -> {}
                CallState.StreamsRunning -> {}
                CallState.Connected ->  callDuration.value = Cans.durationTime
                CallState.Error -> {}
                CallState.CallEnd -> {}
                CallState.MissCall -> {}
                CallState.Unknown -> {}
            }
        }

        override fun onLastCallEnded() {
            Log.i("[CallsViewModel]", "onLastCallEnded")
            isCallEnd.value = true
        }

        override fun onAudioDeviceChanged() {
            AudioRouteUtils.isBluetoothAudioRouteCurrentlyUsed()
        }

        override fun onAudioDevicesListUpdated() {
            Log.i("[CallsViewModel onAudioUpdate]", "Audio devices")

            isBluetooth.value = false
            if (isHeadsetAudioRouteAvailable()) {
                AudioRouteUtils.routeAudioToHeadset()
            } else {
                if (AudioRouteUtils.isBluetoothAudioRouteAvailable()) {
                    AudioRouteUtils.routeAudioToBluetooth()
                    isBluetooth.value = true
                }
            }
        }
    }

    init {
        Cans.addListener(listener)
        callDuration.value = Cans.durationTime
    }

    fun toggleSpeaker() {
        if (AudioRouteUtils.isSpeakerAudioRouteCurrentlyUsed()) {
            forceEarpieceAudioRoute()
        } else {
            forceSpeakerAudioRoute()
        }
    }

    private fun forceEarpieceAudioRoute() {
        if (isHeadsetAudioRouteAvailable()) {
            Log.i("[CansSDK Controls]", "Headset found, route audio to it instead of earpiece")
            AudioRouteUtils.routeAudioToHeadset()
        } else {
            AudioRouteUtils.routeAudioToEarpiece()
        }
    }

    fun forceSpeakerAudioRoute() {
        AudioRouteUtils.routeAudioToSpeaker()
    }

    fun forceBluetoothAudioRoute() {
        if (AudioRouteUtils.isBluetoothAudioRouteAvailable()) {
            AudioRouteUtils.routeAudioToBluetooth()
        }
    }

    override fun onCleared() {
        Cans.removeListener(listener)
        super.onCleared()
    }
}
