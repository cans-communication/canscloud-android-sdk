package cc.cans.canscloud.demoappinsdk.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.models.RegisterState

class OutgoingViewModel : ViewModel() {
    var isCallEnd = MutableLiveData<Boolean>()

    private val listener = object : CansListenerStub {
        override fun onRegistration(state: RegisterState, message: String?) {
            Log.i("[OutgoingViewModel]","onRegistration ${state}")
        }

        override fun onUnRegister() {
        }

        override fun onCallState(state: CallState, message: String?) {
            Log.i("[OutgoingViewModel] onCallState: ","$state")
            when (state) {
                CallState.CallOutgoing -> {}
                CallState.LastCallEnd -> isCallEnd.value = true
                CallState.IncomingCall -> {}
                CallState.StartCall -> {}
                CallState.Connected -> {}
                CallState.Error -> {}
                CallState.CallEnd -> {}
                CallState.MissCall -> {}
                CallState.Unknown -> {}
            }
        }
    }

    init {
        Cans.addListener(listener)
    }

    override fun onCleared() {
        Cans.removeListener(listener)
        super.onCleared()
    }
}
