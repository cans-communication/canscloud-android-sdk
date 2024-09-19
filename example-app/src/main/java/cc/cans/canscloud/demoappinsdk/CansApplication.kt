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
package cc.cans.canscloud.demoappinsdk

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.LifecycleObserver
import cc.cans.canscloud.demoappinsdk.core.CoreContext
import cc.cans.canscloud.demoappinsdk.notifaication.NotificationsManager
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.sdk.core.CoreContextSDK
import org.linphone.core.tools.Log

class CansApplication : Application(), LifecycleObserver {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var coreContext: CoreContextSDK
    }

    override fun onCreate() {
        super.onCreate()

        val appName = getString(R.string.app_name)
        android.util.Log.i("[$appName]", "Application is being created")
        Cans.config(applicationContext)
        CoreContext(this)
        coreContext = CoreContextSDK(this)
        coreContext.start()
        NotificationsManager(this)
        Log.i("[Application] Created")
    }
}
