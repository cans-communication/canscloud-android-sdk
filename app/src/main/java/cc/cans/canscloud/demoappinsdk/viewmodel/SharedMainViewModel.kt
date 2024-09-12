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
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.models.RegisterState

class SharedMainViewModel : ViewModel() {
    val missedCallsCount = MutableLiveData<Int>()
    val statusRegister = MutableLiveData<Int>()
    val isRegister = MutableLiveData<Boolean>()

    private val listener = object : CansListenerStub {
        override fun onRegistration(state: RegisterState, message: String) {
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
            if (Cans.username.isEmpty()) {
                statusRegister.value = R.string.un_register
                isRegister.value = false
            }
        }

        override fun onCallState(state: CallState, message: String) {
            Log.i("[SharedMainViewModel] onCallState: ","$state")
            when (state) {
                CallState.CallOutgoing -> {}
                CallState.LastCallEnd -> {}
                CallState.IncomingCall -> {}
                CallState.StartCall -> {}
                CallState.Connected -> {}
                CallState.Error -> updateMissedCallCount()
                CallState.CallEnd -> updateMissedCallCount()
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

    fun updateMissedCallCount() {
        missedCallsCount.value = Cans.missedCallsCount
    }

    fun register(){
        Cans.addListener(listener)
    }

    fun unregister(){
        Cans.removeAccount()
        Cans.removeListener(listener)
    }
}
