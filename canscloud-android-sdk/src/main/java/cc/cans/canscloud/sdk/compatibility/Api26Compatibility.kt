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
package cc.cans.canscloud.sdk.compatibility

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.Keep
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import cc.cans.canscloud.sdk.telecom.NativeCallWrapper
import org.linphone.core.tools.Log

class Api26Compatibility {
    @Keep
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

        fun getChannelImportance(
            notificationManager: NotificationManagerCompat,
            channelId: String,
        ): Int {
            val channel = notificationManager.getNotificationChannel(channelId)
            return channel?.importance ?: NotificationManagerCompat.IMPORTANCE_NONE
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

        fun startForegroundService(context: Context, intent: Intent) {
            context.startForegroundService(intent)
        }

        @SuppressLint("MissingPermission")
        fun eventVibration(vibrator: Vibrator) {
            val effect = VibrationEffect.createWaveform(longArrayOf(0L, 100L, 100L), intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0), -1)
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()
            vibrator.vibrate(effect, audioAttrs)
        }
    }
}
