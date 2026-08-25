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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import cc.cans.canscloud.sdk.R
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter
import org.linphone.core.tools.Log
import org.linphone.core.tools.service.CoreService

class CoreService : CoreService() {
    override fun onCreate() {
        // Must run before super.onCreate(): Android 8+ requires startForeground() within 5s,
        // and super.onCreate() blocks on Linphone JNI init long enough to blow that window
        // (ForegroundServiceDidNotStartInTimeException).
        startEarlyForeground()
        super.onCreate()
        cansCenter().coreContext.notificationsManager.service = this
        Log.i("[Service] Created")
    }

    private fun startEarlyForeground() {
        val channelId = getString(R.string.notification_channel_service_id)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channelName = getString(R.string.notification_channel_service_name)
            nm?.createNotificationChannel(
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW).apply {
                    enableVibration(false)
                    enableLights(false)
                    setShowBadge(false)
                },
            )
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.notification_channel_service_name))
            .setContentText(getString(R.string.service_description))
            .setSmallIcon(R.drawable.topbar_call_notification)
            .setOngoing(true)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
            } else {
                startForeground(1, notification)
            }
        } catch (e: Exception) {
            Log.e("[Service] Failed to start early foreground: $e")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Defensive re-arm of the 5-second startForeground() rule: covers the service already
        // running (onCreate skipped) and cases where a later startForeground() call silently
        // fails (e.g. missing DATA_SYNC type on Android 15+). Uses PHONE_CALL type, which is
        // always declared.
        startEarlyForeground()

        if (cansCenter().corePreferences.keepServiceAlive) {
            Log.i("[Service] Starting as foreground to keep app alive in background")
            cansCenter().coreContext.notificationsManager.startForegroundToKeepAppAlive(this, false)
        } else if (intent?.extras?.get("StartForeground") == true) {
            Log.i("[Service] Starting as foreground due to device boot or app update")
            cansCenter().coreContext.notificationsManager.startForegroundToKeepAppAlive(this, true)
            cansCenter().coreContext.checkIfForegroundServiceNotificationCanBeRemovedAfterDelay(5000)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun createServiceNotificationChannel() {
        // Done elsewhere
    }

    override fun showForegroundServiceNotification(isVideoCall: Boolean) {
        Log.i("[Service] Starting service as foreground")
        cansCenter().coreContext.notificationsManager.startCallForeground(this)
    }

    override fun hideForegroundServiceNotification() {
        Log.i("[Service] Stopping service as foreground")
        cansCenter().coreContext.notificationsManager.stopCallForeground()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Hang up any live call before the keepServiceAlive check below: that flag only governs
        // whether registration/incoming-call listening survives the swipe-away, not active
        // calls. Doing it here — not just in the app-side CallActionService.onTaskRemoved —
        // avoids a race where core.stop() below tears down the SIP transport before a
        // queued BYE can flush, silently orphaning the call.
        if (cansCenter().core.callsNb > 0) {
            Log.i("[Service] Task removed with active call(s), terminating")
            cansCenter().terminateAllCalls()
        }

        if (!cansCenter().corePreferences.keepServiceAlive) {
            if (cansCenter().core.isInBackground) {
                Log.i("[Service] Task removed, stopping Core")
                cansCenter().coreContext.stop()
            } else {
                Log.w("[Service] Task removed but Core in not in background, skipping")
            }
        } else {
            Log.i("[Service] Task removed but we were asked to keep the service alive, so doing nothing")
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Log.i("[Service] Stopping")
        cansCenter().coreContext.notificationsManager.service = null
        try {
            super.onDestroy()
        } catch (e: IllegalArgumentException) {
            if (e.message.orEmpty().contains("Receiver not registered", ignoreCase = true)) {
                Log.w("[Service] onDestroy: receiver not registered, ignoring", e)
            } else {
                Log.e("[Service] onDestroy: unexpected IllegalArgumentException", e)
                throw e
            }
        }
    }
}
