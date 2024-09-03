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
import cc.cans.canscloud.sdk.callback.CallCallback
import cc.cans.canscloud.sdk.callback.RegisterCallback

class SharedMainViewModel : ViewModel() {
    val missedCallsCount = MutableLiveData<Int>()
    val statusRegister = MutableLiveData<Int>()
    val callDuration = MutableLiveData<Int>()

    private val coreListener = object : CallCallback {
        override fun onCallOutGoing() {
            Log.i("Cans Center","onCallOutGoing")
        }

        override fun onCallEnd() {
            updateMissedCallCount()
            Log.i("Cans Center","onCallEnd")
        }

        override fun onCall() {
            Log.i("Cans Center","onCall")
        }

        override fun onStartCall() {
            Log.i("Cans Center","onStartCall")
        }

        override fun onConnected() {
            callDuration.value =  Cans.durationTime()
            Log.i("Cans Center","onConnected")
        }

        override fun onError(message: String) {
            updateMissedCallCount()
            Log.i("Cans Center","onError")
        }

        override fun onLastCallEnd() {
            Log.i("Cans Center","onLastCallEnd")
        }

        override fun onInComingCall() {
            Log.i("Cans Center","onInComingCall")
        }
    }


    private val registerListener = object : RegisterCallback {
        override fun onRegistrationOk() {
            statusRegister.value = R.string.register_success
            Log.i("Cans Center","onRegistrationOk")
        }

        override fun onRegistrationFail(message: String) {
            statusRegister.value = R.string.register_fail
            Log.i("Cans Center","onRegistrationFail")
        }

        override fun onUnRegister() {
            statusRegister.value = R.string.un_register
            Log.i("Cans Center","onUnRegister")
        }

    }

    init {
        Cans.registerCallListener(coreListener)
        Cans.registersListener(registerListener)
    }

    override fun onCleared() {
        Cans.unCallListener(coreListener)

        super.onCleared()
    }

    fun updateMissedCallCount() {
        missedCallsCount.value = Cans.missedCallsCount
    }

    fun register(){
        Cans.registerCallListener(coreListener)
        Cans.registersListener(registerListener)
    }

    fun unregister(){
        Cans.unCallListener(coreListener)
        Cans.unRegisterListener(registerListener)
    }
}
