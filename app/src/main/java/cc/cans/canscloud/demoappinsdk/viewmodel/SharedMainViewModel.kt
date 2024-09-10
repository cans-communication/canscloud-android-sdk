/*
 * Copyright (c) 2010-2020 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cc.cans.canscloud.demoappinsdk.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cc.cans.canscloud.demoappinsdk.R
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.sdk.callback.CallListeners
import cc.cans.canscloud.sdk.callback.RegisterListeners
import cc.cans.canscloud.sdk.models.CallState

class SharedMainViewModel : ViewModel() {
    val missedCallsCount = MutableLiveData<Int>()
    val statusRegister = MutableLiveData<Int>()

    private val callListener = object : CallListeners {
        override fun onCallState(state: CallState, message: String) {
            Log.i("[SharedMainViewModel] onCallState: ","$state")
            when (state) {
                CallState.CAllOUTGOING -> {}
                CallState.LASTCALLEND -> {}
                CallState.INCOMINGCALL -> {}
                CallState.STARTCALL -> {}
                CallState.CONNECTED -> {}
                CallState.ERROR -> updateMissedCallCount()
                CallState.CALLEND -> updateMissedCallCount()
                CallState.UNKNOWN -> {}
            }
        }
    }


    private val registerListener = object : RegisterListeners {
        override fun onRegistrationOk() {
            statusRegister.value = R.string.register_success
            Log.i("[SharedMainViewModel]","onRegistrationOk")
        }

        override fun onRegistrationFail(message: String) {
            statusRegister.value = R.string.register_fail
            Log.i("[SharedMainViewModel]","onRegistrationFail")
        }

        override fun onUnRegister() {
            statusRegister.value = R.string.un_register
            Log.i("[SharedMainViewModel]","onUnRegister")
        }

    }

    init {
        Cans.setOnCallListeners(callListener)
        Cans.registerListeners.add(registerListener)
    }

    override fun onCleared() {
        Cans.removeCallListeners()

        super.onCleared()
    }

    fun updateMissedCallCount() {
        missedCallsCount.value = Cans.missedCallsCount
    }

    fun register(){
        Cans.setOnCallListeners(callListener)
        Cans.registerListeners.add(registerListener)
    }

    fun unregister(){
        Cans.removeCallListeners()
        Cans.removeAccount()
    }
}
