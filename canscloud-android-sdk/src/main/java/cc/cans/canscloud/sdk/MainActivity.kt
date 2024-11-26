package cc.cans.canscloud.sdk

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import cc.cans.canscloud.sdk.compatibility.Compatibility
import cc.cans.canscloud.sdk.core.CoreContextSDK
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter
import cc.cans.canscloud.sdk.telecom.TelecomHelper
import cc.cans.canscloud.sdk.utils.PermissionHelper

class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
       // checkPermissions()
    }

    private fun checkPermissions() {
        if (!PermissionHelper.singletonHolder().get().hasReadPhoneStatePermission()) {
            Log.i("[$TAG]","Asking for READ_PHONE_STATE permission")
            requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE , Manifest.permission.RECORD_AUDIO), 0)
        } else if (!PermissionHelper.singletonHolder().get().hasPostNotificationsPermission()) {
            // Don't check the following the previous permission is being asked
            Log.i("[$TAG]","Asking for POST_NOTIFICATIONS permission")
            Compatibility.requestPostNotificationsPermission(this, 2)
        } else {
            // Don't check the following the previous permissions are being asked
            checkTelecomManagerPermissions()
        }

        // See https://developer.android.com/about/versions/14/behavior-changes-14#fgs-types
        if (Build.VERSION.SDK_INT >= (Build.VERSION_CODES.UPSIDE_DOWN_CAKE)) {
            val fullScreenIntentPermission = Compatibility.hasFullScreenIntentPermission(
                this
            )
            Log.i("[$TAG]"," Android 14 or above detected: full-screen intent permission is ${if (fullScreenIntentPermission) "granted" else "not granted"}"
            )
            if (!fullScreenIntentPermission) {
                Compatibility.requestFullScreenIntentPermission(this)
            }
        }
    }

    private fun checkTelecomManagerPermissions() {
        if (!cansCenter().corePreferences.useTelecomManager) {
            Log.i("[$TAG]","Telecom Manager feature is disabled")
            if (cansCenter().corePreferences.manuallyDisabledTelecomManager) {
                Log.w("[$TAG]"," User has manually disabled Telecom Manager feature")
            } else {
                if (Compatibility.hasTelecomManagerPermissions(this)) {
                    enableTelecomManager()
                } else {
                    Log.i("[$TAG]"," Asking for Telecom Manager permissions")
                    Compatibility.requestTelecomManagerPermissions(this, 1)
                }
            }
        } else {
            Log.i("[$TAG]"," Telecom Manager feature is already enabled")
        }
    }

    private fun enableTelecomManager() {
        Log.i("[$TAG]"," Telecom Manager permissions granted")
        if (! TelecomHelper.singletonHolder().exists()) {
            Log.i("[$TAG]"," Creating Telecom Helper")
            if (Compatibility.hasTelecomManagerFeature(this)) {
                TelecomHelper.singletonHolder().create(this)
            } else {
                Log.e(
                    "[$TAG]"," Telecom Helper can't be created, device doesn't support connection service!"
                )
            }
        } else {
            Log.e("[$TAG]"," Telecom Manager was already created ?!")
        }
        cansCenter().corePreferences.useTelecomManager = true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.i("[onRequestPermissionsResult]", "RequestCode: $requestCode")

        when (requestCode) {
            0 -> handleReadPhoneStatePermission(grantResults)
            1 -> handleTelecomManagerPermissions(grantResults)
            2 -> handlePostNotificationsPermission(grantResults)
            3 -> handleAudioPermissions(grantResults)
            else -> Log.w("[onRequestPermissionsResult]", "Unhandled request code: $requestCode")
        }
    }

    private fun handleReadPhoneStatePermission(grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i("[onRequestPermissionsResult]", "READ_PHONE_STATE permission granted")
            CoreContextSDK(cansCenter().context).initPhoneStateListener()
            checkPermissions() // Continue checking for other permissions
        } else {
            Log.w("[onRequestPermissionsResult]", "READ_PHONE_STATE permission denied")
        }
    }

    private fun handleTelecomManagerPermissions(grantResults: IntArray) {
        val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        if (allGranted) {
            Log.i("[onRequestPermissionsResult]", "Telecom Manager permissions granted")
            enableTelecomManager()
        } else {
            Log.w("[onRequestPermissionsResult]", "Telecom Manager permissions denied")
        }
    }

    private fun handlePostNotificationsPermission(grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i("[onRequestPermissionsResult]", "POST_NOTIFICATIONS permission granted")
            checkTelecomManagerPermissions()
        } else {
            Log.w("[onRequestPermissionsResult]", "POST_NOTIFICATIONS permission denied")
        }
    }

    private fun handleAudioPermissions(grantResults: IntArray) {
        val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        if (allGranted) {
            Log.i("[onRequestPermissionsResult]", "Audio permissions granted")
            // Implement audio permission granted logic here
        } else {
            Log.w("[onRequestPermissionsResult]", "Audio permissions denied")
        }
    }

}