package cc.cans.canscloud.demoappinsdk.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cc.cans.canscloud.demoappinsdk.R
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter
import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.models.ConferenceState
import cc.cans.canscloud.sdk.models.RegisterState

class SharedMainViewModel : ViewModel() {
    val missedCallsCount = MutableLiveData<Int>()
    val statusRegister = MutableLiveData<Int>()
    val isRegister = MutableLiveData<Boolean>()

    private val listener = object : CansListenerStub {
        override fun onRegistration(state: RegisterState, message: String?) {
            Log.i("[SharedMainViewModel]","onRegistration ${state}")
            when (state) {
                RegisterState.OK -> {
                    statusRegister.value = R.string.register_success
                    isRegister.value = true
                }
                RegisterState.FAIL -> {
                    statusRegister.value = R.string.register_fail
                    isRegister.value = false
                }

                RegisterState.None -> {
                    statusRegister.value = R.string.un_register
                    isRegister.value = false
                }
            }
        }

        override fun onUnRegister() {
            if (cansCenter().username.isEmpty()) {
                statusRegister.value = R.string.un_register
                isRegister.value = false
            }
        }

        override fun onCallState(state: CallState, message: String?) {
            Log.i("[SharedMainViewModel] onCallState: ","$state")
            when (state) {
                CallState.Idle -> {}
                CallState.IncomingCall -> {}
                CallState.StartCall -> {}
                CallState.CallOutgoing -> {}
                CallState.StreamsRunning -> {}
                CallState.Connected -> {}
                CallState.Error -> updateMissedCallCount()
                CallState.CallEnd -> updateMissedCallCount()
                CallState.MissCall -> {}
                CallState.Pause -> {}
                CallState.Resuming -> {}
                CallState.Unknown -> {}
            }
        }

        override fun onLastCallEnded() {
            Log.i("[SharedMainViewModel]", "onLastCallEnded")
        }

        override fun onAudioDeviceChanged() {
            Log.i("[SharedMainViewModel onAudioUpdate]", "onAudioDeviceChanged")
        }

        override fun onAudioDevicesListUpdated() {
            Log.i("[SharedMainViewModel onAudioUpdate]", "onAudioDevicesListUpdated")
        }

        override fun onConferenceState(state: ConferenceState) {
        }
    }

    init {
        cansCenter().addListener(listener)
    }

    override fun onCleared() {
        cansCenter().removeListener(listener)
        super.onCleared()
    }

    fun updateMissedCallCount() {
        missedCallsCount.value = cansCenter().missedCallsCount
    }

    fun register(){
        cansCenter().addListener(listener)
    }

    fun unregister(){
        cansCenter().removeAccount()
    }
}
