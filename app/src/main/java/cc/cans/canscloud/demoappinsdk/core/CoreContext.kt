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
package cc.cans.canscloud.demoappinsdk.core

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import cc.cans.canscloud.demoappinsdk.call.CallActivity
import cc.cans.canscloud.demoappinsdk.call.IncomingActivity
import cc.cans.canscloud.demoappinsdk.call.OutgoingActivity
import cc.cans.canscloud.demoappinsdk.notifaication.NotificationsManager
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.sdk.Cans.Companion.corePreferences
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.models.RegisterState

class CoreContext(
    val context: Context
) : LifecycleOwner, ViewModelStoreOwner {
    private val _lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = _lifecycleRegistry

    private val _viewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore
        get() = _viewModelStore

    val notificationsManager: NotificationsManager by lazy {
        NotificationsManager(context)
    }

    private val listener = object : CansListenerStub {
        override fun onRegistration(state: RegisterState, message: String) {
            Log.i("[SharedMainViewModel]","onRegistration ${state}")
        }

        override fun onUnRegister() {
            Log.i("[Context]","onUnRegistration")
        }

        override fun onCallState(state: CallState, message: String) {
            Log.i("[Context] onCallState: ","$state")
            when (state) {
                CallState.CallOutgoing -> onOutgoingStarted()
                CallState.LastCallEnd -> {}
                CallState.IncomingCall -> onIncomingReceived()
                CallState.StartCall -> {}
                CallState.Connected -> onCallStarted()
                CallState.Error -> {}
                CallState.CallEnd -> {}
                CallState.Unknown -> {}
            }
        }
    }

    init {
        Cans.coreListeners.add(listener)
        notificationsManager.onCoreReady()

        _lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        Log.i("[Context]","Ready")
    }

    /* Start call related activities */

    private fun onIncomingReceived() {
        if (corePreferences.preventInterfaceFromShowingUp) {
            Log.w("[Context]","We were asked to not show the incoming call screen")
            return
        }

        Log.i("[Context]","Starting IncomingCallActivity")
        val intent = Intent(context, IncomingActivity::class.java)
        // This flag is required to start an Activity from a Service context
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun onOutgoingStarted() {
        if (corePreferences.preventInterfaceFromShowingUp) {
            Log.w("[Context]","We were asked to not show the outgoing call screen")
            return
        }

        Log.i("[Context]","Starting OutgoingCallActivity")
        val intent = Intent(context, OutgoingActivity::class.java)
        // This flag is required to start an Activity from a Service context
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun onCallStarted() {
        if (corePreferences.preventInterfaceFromShowingUp) {
            Log.w("[Context]","We were asked to not show the call screen")
            return
        }

        Log.i("[Context]","Starting CallActivity")
        val intent = Intent(context, CallActivity::class.java)
        // This flag is required to start an Activity from a Service context
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        context.startActivity(intent)
    }
}
