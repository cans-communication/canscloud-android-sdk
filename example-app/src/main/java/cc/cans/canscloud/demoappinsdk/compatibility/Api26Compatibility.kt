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
package cc.cans.canscloud.demoappinsdk.compatibility

import android.Manifest
import android.app.*
import android.content.Context
import android.content.pm.PackageManager
import androidx.fragment.app.Fragment
import cc.cans.canscloud.demoappinsdk.telecom.NativeCallWrapper
import org.linphone.core.tools.Log

class Api26Compatibility {
    companion object {
        fun changeAudioRouteForTelecomManager(connection: NativeCallWrapper, route: Int): Boolean {
            Log.i("[Telecom Helper] Changing audio route [$route] on connection ${connection.callId}")

            val audioState = connection.callAudioState
            if (audioState != null && audioState.route == route) {
                Log.w("[Telecom Helper] Connection is already using this route")
                return false
            } else if (audioState == null) {
                Log.w("[Telecom Helper] Failed to retrieve connection's call audio state!")
                return false
            }

            connection.setAudioRoute(route)
            return true
        }

        fun requestTelecomManagerPermission(activity: Activity, code: Int) {
            activity.requestPermissions(
                arrayOf(
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.MANAGE_OWN_CALLS,
                ),
                code,
            )
        }

        fun requestTelecomManagerPermissionFragment(fragment: Fragment, code: Int) {
            fragment.requestPermissions(
                arrayOf(
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.MANAGE_OWN_CALLS,
                ),
                code,
            )
        }

        fun hasTelecomManagerPermission(context: Context): Boolean {
            return Compatibility.hasPermission(context, Manifest.permission.READ_PHONE_STATE) &&
                Compatibility.hasPermission(context, Manifest.permission.MANAGE_OWN_CALLS)
        }

        fun hasTelecomManagerFeature(context: Context): Boolean {
            return context.packageManager.hasSystemFeature(
                PackageManager.FEATURE_CONNECTION_SERVICE
            )
        }
    }
}
