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

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.annotation.Keep
import androidx.fragment.app.Fragment
import cc.cans.canscloud.sdk.telecom.NativeCallWrapper
import org.linphone.mediastream.Version

@Suppress("DEPRECATION")
class Compatibility {
    @Keep
    companion object {
        const val BLUETOOTH_CONNECT = "android.permission.BLUETOOTH_CONNECT"

        @JvmStatic
        fun hasPermission(context: Context, permission: String): Boolean {
            return when (Version.sdkAboveOrEqual(Version.API23_MARSHMALLOW_60)) {
                true -> Api23Compatibility.hasPermission(context, permission)
                else -> context.packageManager.checkPermission(permission, context.packageName) == PackageManager.PERMISSION_GRANTED
            }
        }

        // See https://developer.android.com/about/versions/11/privacy/permissions#phone-numbers
        @JvmStatic
        fun hasReadPhoneStateOrNumbersPermission(context: Context): Boolean {
            return if (Version.sdkAboveOrEqual(Version.API30_ANDROID_11)) {
                Api30Compatibility.hasReadPhoneNumbersPermission(context)
            } else {
                Api29Compatibility.hasReadPhoneStatePermission(context)
            }
        }

        @JvmStatic
        fun hasTelecomManagerPermissions(context: Context): Boolean {
            return if (Version.sdkAboveOrEqual(Version.API30_ANDROID_11)) {
                Api30Compatibility.hasTelecomManagerPermission(context)
            } else if (Version.sdkAboveOrEqual(Version.API29_ANDROID_10)) {
                Api29Compatibility.hasTelecomManagerPermission(context)
            } else {
                false
            }
        }

        @JvmStatic
        fun requestTelecomManagerPermissions(activity: Activity, code: Int) {
            if (Version.sdkAboveOrEqual(Version.API30_ANDROID_11)) {
                Api30Compatibility.requestTelecomManagerPermission(activity, code)
            } else {
                Api26Compatibility.requestTelecomManagerPermission(activity, code)
            }
        }

        @JvmStatic
        fun requestTelecomManagerPermissionsFragment(fragment: Fragment, code: Int) {
            if (Version.sdkAboveOrEqual(Version.API30_ANDROID_11)) {
                Api30Compatibility.requestTelecomManagerPermissionFragment(fragment, code)
            } else {
                Api26Compatibility.requestTelecomManagerPermissionFragment(fragment, code)
            }
        }

        @JvmStatic
        fun hasTelecomManagerFeature(context: Context): Boolean {
            if (Version.sdkAboveOrEqual(Version.API33_ANDROID_13_TIRAMISU)) {
                return Api33Compatibility.hasTelecomManagerFeature(context)
            } else if (Version.sdkAboveOrEqual(Version.API26_O_80)) {
                return Api26Compatibility.hasTelecomManagerFeature(context)
            }
            return false
        }

        @JvmStatic
        fun requestPostNotificationsPermission(activity: Activity, code: Int) {
            if (Version.sdkAboveOrEqual(Version.API33_ANDROID_13_TIRAMISU)) {
                Api33Compatibility.requestPostNotificationsPermission(activity, code)
            }
        }

        @JvmStatic
        fun requestPostNotificationsPermission(fragment: Fragment, code: Int) {
            if (Version.sdkAboveOrEqual(Version.API33_ANDROID_13_TIRAMISU)) {
                Api33Compatibility.requestPostNotificationsPermission(fragment, code)
            }
        }

        @JvmStatic
        fun hasPostNotificationsPermission(context: Context): Boolean {
            return if (Version.sdkAboveOrEqual(Version.API33_ANDROID_13_TIRAMISU)) {
                Api33Compatibility.hasPostNotificationsPermission(context)
            } else {
                true
            }
        }

        @JvmStatic
        fun hasBluetoothConnectPermission(context: Context): Boolean {
            if (Version.sdkAboveOrEqual(Version.API31_ANDROID_12)) {
                return Api31Compatibility.hasBluetoothConnectPermission(context)
            }
            return true
        }

        @JvmStatic
        fun hasFullScreenIntentPermission(context: Context): Boolean {
            if (Version.sdkAboveOrEqual(Version.API34_ANDROID_14_UPSIDE_DOWN_CAKE)) {
                return Api34Compatibility.hasFullScreenIntentPermission(context)
            }
            return true
        }

        @JvmStatic
        fun requestFullScreenIntentPermission(context: Context): Boolean {
            if (Version.sdkAboveOrEqual(Version.API34_ANDROID_14_UPSIDE_DOWN_CAKE)) {
                Api34Compatibility.requestFullScreenIntentPermission(context)
                return true
            }
            return false
        }

        @JvmStatic
        fun createPhoneListener(telephonyManager: TelephonyManager): PhoneStateInterface {
            return if (Version.sdkStrictlyBelow(Version.API31_ANDROID_12)) {
                PhoneStateListener(telephonyManager)
            } else {
                TelephonyListener(telephonyManager)
            }
        }

        @JvmStatic
        fun changeAudioRouteForTelecomManager(connection: NativeCallWrapper, route: Int): Boolean {
            if (Version.sdkAboveOrEqual(Version.API26_O_80)) {
                return Api26Compatibility.changeAudioRouteForTelecomManager(connection, route)
            }
            return false
        }
    }
}
