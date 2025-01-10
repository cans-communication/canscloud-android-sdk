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
package cc.cans.canscloud.sdk.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cc.cans.canscloud.sdk.R
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.sdk.compatibility.Compatibility
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter
import cc.cans.canscloud.sdk.models.CallState
import org.linphone.core.*
import org.linphone.core.tools.Log

class NotificationsManager(private val context: Context) {
    companion object {
        private const val SERVICE_NOTIF_ID = 1
    }

    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    private var currentForegroundServiceNotificationId: Int = 0
    private var serviceNotification: Notification? = null

    var service: CoreService? = null
    lateinit var callState: CallState

    /* Service related */

    fun startForeground() {
        val serviceChannel = cansCenter().context.getString(R.string.notification_channel_service_id)
        if (Compatibility.getChannelImportance(notificationManager, serviceChannel) == NotificationManagerCompat.IMPORTANCE_NONE) {
            Log.w("[Notifications Manager] Service channel is disabled!")
            return
        }

        val coreService = service
        if (coreService != null) {
            startForeground(coreService)
        } else {
            Log.w(
                "[Notifications Manager] Can't start service as foreground without a service, starting it now",
            )
            val intent = Intent()
            intent.setClass(cansCenter().context, CoreService::class.java)
            try {
                cansCenter().context.startForegroundService(intent)
            } catch (ise: IllegalStateException) {
                Log.e("[Notifications Manager] Failed to start Service: $ise")
            } catch (se: SecurityException) {
                Log.e("[Notifications Manager] Failed to start Service: $se")
            }
        }
    }

    private var listeners = mutableListOf<CansListenerStub>()

    fun startCallForeground(coreService: CoreService) {
        service = coreService
        when {
            currentForegroundServiceNotificationId != 0 -> {
                if (currentForegroundServiceNotificationId != SERVICE_NOTIF_ID) {
                    Log.e(
                        "[Notifications Manager] There is already a foreground service notification [$currentForegroundServiceNotificationId]",
                    )
                } else {
                    Log.i(
                        "[Notifications Manager] There is already a foreground service notification, no need to use the call notification to keep Service alive",
                    )
                }
            }
            cansCenter().core.callsNb > 0 -> {
                // When this method will be called, we won't have any notification yet
                val call = cansCenter().core.currentCall ?: cansCenter().core.calls[0]
                when (call.state) {
                    Call.State.IncomingEarlyMedia, Call.State.IncomingReceived -> {
                        setListenerCall(CallState.IncomingCall)
                    }

                    Call.State.OutgoingInit -> {
                        setListenerCall(CallState.StartCall)
                    }

                    Call.State.OutgoingProgress -> {
                        setListenerCall(CallState.CallOutgoing)
                    }

                    Call.State.StreamsRunning -> {
                        setListenerCall(CallState.StreamsRunning)
                    }

                    Call.State.Connected -> {
                        setListenerCall(CallState.Connected)
                    }

                    Call.State.Error -> {
                        setListenerCall(CallState.Error)
                    }

                    Call.State.End -> {
                        setListenerCall(CallState.CallEnd)
                    }

                    Call.State.Released -> {
                        setListenerCall(CallState.MissCall)
                    }

                    else -> {
                        setListenerCall(CallState.Unknown)
                    }
                }
            }
        }
    }

    private fun setListenerCall(callState: CallState) {
        this.callState = callState
        listeners.forEach { it.onCallState(callState) }
    }

    private fun startForeground(coreService: CoreService) {
        service = coreService

        val notification = serviceNotification ?: createServiceNotification()
        if (notification == null) {
            Log.e(
                "[Notifications Manager] Failed to create service notification, aborting foreground service!",
            )
            return
        }

        currentForegroundServiceNotificationId = SERVICE_NOTIF_ID
        Log.i(
            "[Notifications Manager] Starting service as foreground [$currentForegroundServiceNotificationId]",
        )

        val core = cansCenter().core
        val isActiveCall = if (core.callsNb > 0) {
            val currentCall = core.currentCall ?: core.calls.first()
            when (currentCall.state) {
                Call.State.IncomingReceived, Call.State.IncomingEarlyMedia, Call.State.OutgoingInit, Call.State.OutgoingProgress, Call.State.OutgoingRinging -> false
                else -> true
            }
        } else {
            false
        }

        Compatibility.startDataSyncForegroundService(
            coreService,
            currentForegroundServiceNotificationId,
            notification,
            isActiveCall,
        )
    }

    fun startForegroundToKeepAppAlive(
        coreService: CoreService,
        useAutoStartDescription: Boolean = true,
    ) {
        service = coreService

        val notification = serviceNotification ?: createServiceNotification()
        if (notification == null) {
            Log.e(
                "[Notifications Manager] Failed to create service notification, aborting foreground service!",
            )
            return
        }

        currentForegroundServiceNotificationId = SERVICE_NOTIF_ID
        Log.i(
            "[Notifications Manager] Starting service as foreground [$currentForegroundServiceNotificationId]",
        )

        Compatibility.startDataSyncForegroundService(
            coreService,
            currentForegroundServiceNotificationId,
            notification,
            false,
        )
    }

    private fun startForeground(
        notificationId: Int,
        callNotification: Notification,
        isCallActive: Boolean,
    ) {
        val coreService = service
        if (coreService != null && (currentForegroundServiceNotificationId == 0 || currentForegroundServiceNotificationId == notificationId)) {
            Log.i(
                "[Notifications Manager] Starting service as foreground using call notification [$notificationId]",
            )
            try {
                currentForegroundServiceNotificationId = notificationId

                Compatibility.startCallForegroundService(
                    coreService,
                    currentForegroundServiceNotificationId,
                    callNotification,
                    isCallActive,
                )
            } catch (e: Exception) {
                Log.e("[Notifications Manager] Foreground service wasn't allowed! $e")
                currentForegroundServiceNotificationId = 0
            }
        } else {
            Log.w(
                "[Notifications Manager] Can't start foreground service using notification id [$notificationId] (current foreground service notification id is [$currentForegroundServiceNotificationId]) and service [$coreService]",
            )
        }
    }

    fun stopForegroundNotification() {
        if (service != null) {
            Log.i("[Notifications Manager] Stopping service as foreground [$currentForegroundServiceNotificationId]")
            service?.stopForeground(true)
            currentForegroundServiceNotificationId = 0
        }
    }

    fun stopForegroundNotificationIfPossible() {
        if (service != null && currentForegroundServiceNotificationId == SERVICE_NOTIF_ID && !cansCenter().corePreferences.keepServiceAlive) {
            Log.i("[Notifications Manager] Stopping auto-started service notification [$currentForegroundServiceNotificationId]")
            stopForegroundNotification()
        }
    }

    fun stopCallForeground() {
        if (service != null && currentForegroundServiceNotificationId != SERVICE_NOTIF_ID && !cansCenter().corePreferences.keepServiceAlive) {
            Log.i("[Notifications Manager] Stopping call notification [$currentForegroundServiceNotificationId] used as foreground service")
            stopForegroundNotification()
        }
    }

    private fun createServiceNotification(): Notification? {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(context, "ForegroundServiceChannel")
            .setContentTitle("Service Running")
            .setContentText(cansCenter().context.getString(R.string.service_description))
            .setSmallIcon(R.drawable.topbar_call_notification)

        serviceNotification = notification.build()
        return serviceNotification
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            "ForegroundServiceChannel",
            "Foreground Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            enableVibration(false)
            vibrationPattern = null
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }
}
