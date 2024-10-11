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

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Vibrator
import android.provider.MediaStore
import android.provider.Settings

@Suppress("DEPRECATION")
@TargetApi(21)
class Api21Compatibility {
    companion object {
        @SuppressLint("MissingPermission")
        fun getDeviceName(context: Context): String {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            var name = adapter?.name
            if (name == null) {
                name = Settings.Secure.getString(
                    context.contentResolver,
                    "bluetooth_name"
                )
            }
            if (name == null) {
                name = Build.MANUFACTURER + " " + Build.MODEL
            }
            return name
        }

        @SuppressLint("MissingPermission")
        fun eventVibration(vibrator: Vibrator) {
            val pattern = longArrayOf(0, 100, 100)
            vibrator.vibrate(pattern, -1)
        }

        fun getBitmapFromUri(context: Context, uri: Uri): Bitmap {
            return MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }
}
