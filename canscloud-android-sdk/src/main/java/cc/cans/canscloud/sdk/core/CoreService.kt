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
import cc.cans.canscloud.sdk.callback.CoreServiceListener
import org.linphone.core.tools.Log
import org.linphone.core.tools.service.CoreService

private var coreServiceListenerStub = mutableListOf<CoreServiceListener>()

class CoreService : CoreService() {
    override fun onCreate() {
        super.onCreate()
        coreServiceListenerStub.forEach { it.onCreate() }
        Log.i("[Service] Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        coreServiceListenerStub.forEach { it.onStartCommand() }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun createServiceNotificationChannel() {
        // Done elsewhere
    }

    override fun showForegroundServiceNotification(isVideoCall: Boolean) {
        Log.i("[Service] Starting service as foreground")
        coreServiceListenerStub.forEach { it.showForegroundServiceNotification() }
    }

    override fun hideForegroundServiceNotification() {
        Log.i("[Service] Stopping service as foreground")
        coreServiceListenerStub.forEach { it.hideForegroundServiceNotification() }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        coreServiceListenerStub.forEach { it.onTaskRemoved() }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Log.i("[Service] Stopping")
        coreServiceListenerStub.forEach { it.onDestroy() }
        super.onDestroy()
    }

    fun addListener(listener: CoreServiceListener) {
        coreServiceListenerStub.add(listener)
    }

    fun removeListener(listener: CoreServiceListener) {
        coreServiceListenerStub.remove(listener)
    }

    fun removeAllListener() {
        coreServiceListenerStub.clear()
    }
}
