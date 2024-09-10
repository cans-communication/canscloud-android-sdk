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
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.models.RegisterState

class OutgoingViewModel : ViewModel() {
    var isCallEnd = MutableLiveData<Boolean>()

    private val listener = object : CansListenerStub {
        override fun onRegistration(state: RegisterState, message: String) {
        }

        override fun onUnRegister() {
        }

        override fun onCallState(state: CallState, message: String) {
            Log.i("[OutgoingViewModel] onCallState: ","$state")
            when (state) {
                CallState.CAll_OUTGOING -> {}
                CallState.LAST_CALLEND ->  isCallEnd.value = true
                CallState.INCOMING_CALL -> {}
                CallState.START_CALL ->  {}
                CallState.CONNECTED -> {}
                CallState.ERROR -> {}
                CallState.CALLEND -> {}
                CallState.UNKNOWN -> {}
            }
        }
    }

    init {
        Cans.coreListeners.add(listener)
    }

    override fun onCleared() {
        Cans.coreListeners.remove(listener)
        super.onCleared()
    }
}
