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
package cc.cans.canscloud.sdk.utils

import android.Manifest
import android.content.Context
import cc.cans.canscloud.sdk.compatibility.Compatibility
import org.linphone.core.tools.Log

/**
 * Helper methods to check whether a permission has been granted and log the result
 */
class PermissionHelper private constructor(private val context: Context) {

    companion object {
        private var holder = SingletonHolder<PermissionHelper, Context>(::PermissionHelper)

        @JvmStatic
        fun singletonHolder(): SingletonHolder<PermissionHelper, Context> {
            return holder
        }
    }

    private fun hasPermission(permission: String): Boolean {
        val granted = Compatibility.hasPermission(context, permission)

        if (granted) {
            Log.d("[Permission Helper] Permission $permission is granted")
        } else {
            Log.w("[Permission Helper] Permission $permission is denied")
        }

        return granted
    }

    fun hasReadContactsPermission(): Boolean {
        return hasPermission(Manifest.permission.READ_CONTACTS)
    }

    fun hasWriteContactsPermission(): Boolean {
        return hasPermission(Manifest.permission.WRITE_CONTACTS)
    }

    fun hasReadPhoneStatePermission(): Boolean {
        return hasPermission(Manifest.permission.READ_PHONE_STATE)
    }

    fun hasReadPhoneStateOrPhoneNumbersPermission(): Boolean {
        return Compatibility.hasReadPhoneStateOrNumbersPermission(context)
    }

    fun hasReadExternalStoragePermission(): Boolean {
        return hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun hasWriteExternalStoragePermission(): Boolean {
        return hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    fun hasCameraPermission(): Boolean {
        return hasPermission(Manifest.permission.CAMERA)
    }

    fun hasRecordAudioPermission(): Boolean {
        return hasPermission(Manifest.permission.RECORD_AUDIO)
    }

    fun hasPostNotificationsPermission(): Boolean {
        return Compatibility.hasPostNotificationsPermission(context)
    }

    fun hasBluetoothConnectPermission(): Boolean {
        return Compatibility.hasBluetoothConnectPermission(context)
    }
}
