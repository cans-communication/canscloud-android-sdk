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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.sdk.callback.CallCallback

class CallsViewModel : ViewModel() {
    val callDuration = MutableLiveData<Int>()
    var isCallEnd = MutableLiveData<Boolean>()


    private val coreListener = object : CallCallback {
        override fun onCallEnd() {
        }

        override fun onCall() {
            callDuration.value =  Cans.durationTime()
        }

        override fun onCallOutGoing() {
        }

        override fun onConnected() {
            Log.i("Cans Center","onConnectedCall")
        }

        override fun onError(message: String) {
        }

        override fun onInComingCall() {
        }

        override fun onLastCallEnd() {
            isCallEnd.value = true
            Log.i("Cans Center","onLastCallEnd")
        }

        override fun onStartCall() {
            Log.i("Cans Center","onStartCall")
        }
    }

    init {
        Cans.registerCallListener(coreListener)
    }

    override fun onCleared() {
        Cans.unCallListener(coreListener)

        super.onCleared()
    }
}
