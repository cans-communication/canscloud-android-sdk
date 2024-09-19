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
import android.annotation.TargetApi
import android.content.Context
import cc.cans.canscloud.sdk.compatibility.Compatibility
import org.linphone.core.tools.Log

@TargetApi(29)
class Api29Compatibility {
    companion object {
        fun hasTelecomManagerPermission(context: Context): Boolean {
            return Compatibility.hasPermission(context, Manifest.permission.READ_PHONE_STATE) &&
                    Compatibility.hasPermission(context, Manifest.permission.MANAGE_OWN_CALLS)
        }

        fun hasReadPhoneStatePermission(context: Context): Boolean {
            val granted = Compatibility.hasPermission(context, Manifest.permission.READ_PHONE_STATE)
            if (granted) {
                Log.d("[Permission Helper] Permission READ_PHONE_STATE is granted")
            } else {
                Log.w("[Permission Helper] Permission READ_PHONE_STATE is denied")
            }
            return granted
        }
    }
}
