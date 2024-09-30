package cc.cans.canscloud.demoappinsdk.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cc.cans.canscloud.demoappinsdk.R
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cans
import cc.cans.canscloud.sdk.models.CallState
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
            }
        }

        override fun onUnRegister() {
            if (cans.username.isEmpty()) {
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
    }

    init {
        cans.addListener(listener)
    }

    override fun onCleared() {
        cans.removeListener(listener)
        super.onCleared()
    }

    fun updateMissedCallCount() {
        missedCallsCount.value = cans.missedCallsCount
    }

    fun register(){
        cans.addListener(listener)
    }

    fun unregister(){
        cans.removeAccount()
        cans.removeListener(listener)
    }
}
