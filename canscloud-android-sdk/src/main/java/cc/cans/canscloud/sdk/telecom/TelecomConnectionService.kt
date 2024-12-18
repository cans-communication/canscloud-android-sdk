/*
 * Copyright (c) 2010-2021 Belledonne Communications SARL.
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
package cc.cans.canscloud.sdk.telecom

import android.annotation.TargetApi
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.telecom.*
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.tools.Log

@TargetApi(29)
class TelecomConnectionService : ConnectionService() {
    val TAG = "Telecom Connection Service"
    private val listener: CoreListenerStub = object : CoreListenerStub() {
        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State?,
            message: String,
        ) {
            Log.i("[$TAG] call [${call.callLog.callId}] state changed: $state")
            when (call.state) {
                Call.State.OutgoingProgress -> {
                    for (connection in TelecomHelper.singletonHolder().get().connections) {
                        if (connection.callId.isEmpty()) {
                            Log.i("$TAG] Updating connection with call ID: ${call.callLog.callId}")
                            connection.callId = core.currentCall?.callLog?.callId.orEmpty()
                        }
                    }
                }
                Call.State.Error -> onCallError(call)
                Call.State.End, Call.State.Released -> onCallEnded(call)
                Call.State.Connected -> onCallConnected(call)
                else -> {
                    Log.e("[$TAG] No state call")
                }
            }
        }

        override fun onLastCallEnded(core: Core) {
            val connectionsCount = TelecomHelper.singletonHolder().get().connections.size
            if (connectionsCount > 0) {
                Log.w("[$TAG] Last call ended, there is $connectionsCount connections still alive")
                for (connection in TelecomHelper.singletonHolder().get().connections) {
                    Log.w("[$TAG] Destroying zombie connection ${connection.callId}")
                    connection.setDisconnected(DisconnectCause(DisconnectCause.OTHER))
                    connection.destroy()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        Log.i("[$TAG] onCreate()")
        cansCenter().core.addListener(listener)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i("[$TAG] onUnbind()")
        cansCenter().core.removeListener(listener)

        return super.onUnbind(intent)
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest,
    ): Connection {
        if (cansCenter().core.callsNb == 0) {
            Log.w("[$TAG] No call in Core, aborting outgoing connection!")
            return Connection.createCanceledConnection()
        }

        val accountHandle = request.accountHandle
        val componentName = ComponentName(applicationContext, this.javaClass)
        return if (accountHandle != null && componentName == accountHandle.componentName) {
            Log.i("[$TAG] Creating outgoing connection")

            val extras = request.extras
            var callId = extras.getString("Call-ID")
            val displayName = extras.getString("DisplayName")
            if (callId == null) {
                callId = cansCenter().core.currentCall?.callLog?.callId.orEmpty()
            }
            Log.i("[$TAG] Outgoing connection is for call [$callId] with display name [$displayName]")

            // Prevents user dialing back from native dialer app history
            if (callId.isEmpty() && displayName.isNullOrEmpty()) {
                Log.e("[$TAG] Looks like a call was made from native dialer history, aborting")
                return Connection.createFailedConnection(DisconnectCause(DisconnectCause.OTHER))
            }

            val connection = NativeCallWrapper(callId)
            connection.setDialing()

            val providedHandle = request.address
            connection.setAddress(providedHandle, TelecomManager.PRESENTATION_ALLOWED)
            connection.setCallerDisplayName(displayName, TelecomManager.PRESENTATION_ALLOWED)
            Log.i("[$TAG] Address is $providedHandle")

            TelecomHelper.singletonHolder().get().connections.add(connection)
            connection
        } else {
            Log.e("[$TAG] Error: $accountHandle $componentName")
            Connection.createFailedConnection(
                DisconnectCause(
                    DisconnectCause.ERROR,
                    "Invalid inputs: $accountHandle $componentName",
                ),
            )
        }
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest,
    ): Connection {
        if (cansCenter().core.callsNb == 0) {
            Log.w("[$TAG] No call in Core, aborting incoming connection!")
            return Connection.createCanceledConnection()
        }

        val accountHandle = request.accountHandle
        val componentName = ComponentName(applicationContext, this.javaClass)
        return if (accountHandle != null && componentName == accountHandle.componentName) {
            Log.i("[$TAG] Creating incoming connection")

            val extras = request.extras
            val incomingExtras = extras.getBundle(TelecomManager.EXTRA_INCOMING_CALL_EXTRAS)
            var callId = incomingExtras?.getString("Call-ID")
            val displayName = incomingExtras?.getString("DisplayName")
            if (callId == null) {
                callId = cansCenter().core.currentCall?.callLog?.callId.orEmpty()
            }
            Log.i("[$TAG] Incoming connection is for call [$callId] with display name [$displayName]")

            val connection = NativeCallWrapper(callId)
            connection.setRinging()

            val providedHandle =
                incomingExtras?.getParcelable<Uri>(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS)
            connection.setAddress(providedHandle, TelecomManager.PRESENTATION_ALLOWED)
            connection.setCallerDisplayName(displayName, TelecomManager.PRESENTATION_ALLOWED)
            Log.i("[$TAG] Address is $providedHandle")

            TelecomHelper.singletonHolder().get().connections.add(connection)
            connection
        } else {
            Log.e("[$TAG] Error: $accountHandle $componentName")
            Connection.createFailedConnection(
                DisconnectCause(
                    DisconnectCause.ERROR,
                    "Invalid inputs: $accountHandle $componentName",
                ),
            )
        }
    }

    private fun onCallError(call: Call) {
        val callId = call.callLog.callId
        val connection = TelecomHelper.singletonHolder().get().findConnectionForCallId(callId.orEmpty())
        if (connection == null) {
            Log.e("[$TAG] Failed to find connection for call id: $callId")
            return
        }

        TelecomHelper.singletonHolder().get().connections.remove(connection)
        connection.setDisconnected(DisconnectCause(DisconnectCause.ERROR))
        connection.destroy()
    }

    private fun onCallEnded(call: Call) {
        val callId = call.callLog.callId
        val connection = TelecomHelper.singletonHolder().get().findConnectionForCallId(callId.orEmpty())
        if (connection == null) {
            Log.e("[$TAG] Failed to find connection for call id: $callId")
            return
        }

        TelecomHelper.singletonHolder().get().connections.remove(connection)
        val reason = call.reason
        Log.i("[$TAG] Call [$callId] ended with reason: $reason, destroying connection")
        connection.setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        connection.destroy()
    }

    private fun onCallConnected(call: Call) {
        val callId = call.callLog.callId
        val connection = TelecomHelper.singletonHolder().get().findConnectionForCallId(callId.orEmpty())
        if (connection == null) {
            Log.e("[$TAG] Failed to find connection for call id: $callId")
            return
        }

        if (connection.state != Connection.STATE_HOLDING) {
            connection.setActive()
        }
    }
}
