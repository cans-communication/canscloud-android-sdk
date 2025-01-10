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

import android.content.Intent
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter
import org.linphone.core.tools.Log
import org.linphone.core.tools.service.CoreService

class CoreService : CoreService() {
    override fun onCreate() {
        super.onCreate()
        cansCenter().coreContext.notificationsManager.service = this
        Log.i("[Service] Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
        super.onDestroy()
    }
}
