package cc.cans.canscloud.demoappinsdk

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import cc.cans.canscloud.demoappinsdk.CansApplication.Companion.coreContext
import cc.cans.canscloud.demoappinsdk.compatibility.Compatibility
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.demoappinsdk.databinding.ActivityMainBinding
import cc.cans.canscloud.demoappinsdk.telecom.TelecomHelper
import cc.cans.canscloud.demoappinsdk.utils.PermissionHelper
import cc.cans.canscloud.sdk.Cans.Companion.corePreferences
import cc.cans.canscloud.sdk.models.CansTransport
import org.linphone.core.tools.Log
import org.linphone.mediastream.Version

class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Cans.register(
            "40102",
            "p40102CANS",
            "cns.cans.cc",
            "8446",
            CansTransport.UDP
        )

        checkPermissions()
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 0) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("[Dialer] READ_PHONE_STATE permission has been granted")
                coreContext.initPhoneStateListener()
                // If first permission has been granted, continue to ask for permissions,
                // otherwise don't do it or it will loop indefinitely
                checkPermissions()
            }
        } else if (requestCode == 1) {
            var allGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                }
            }
            if (allGranted) {
                Log.i("[Dialer] Telecom Manager permission have been granted")
                enableTelecomManager()
            } else {
                Log.w("[Dialer] Telecom Manager permission have been denied (at least one of them)")
            }
        } else if (requestCode == 2) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("[Dialer] POST_NOTIFICATIONS permission has been granted")
            }
            checkTelecomManagerPermissions()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun checkPermissions() {
        if (!PermissionHelper.get().hasReadPhoneStatePermission()) {
            Log.i("[$TAG] Asking for READ_PHONE_STATE permission")
            requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), 0)
        } else if (!PermissionHelper.get().hasPostNotificationsPermission()) {
            // Don't check the following the previous permission is being asked
            Log.i("[$TAG] Asking for POST_NOTIFICATIONS permission")
            Compatibility.requestPostNotificationsPermission(this, 2)
        } else if (Version.sdkAboveOrEqual(Version.API26_O_80)) {
            // Don't check the following the previous permissions are being asked
            checkTelecomManagerPermissions()
        }

        // See https://developer.android.com/about/versions/14/behavior-changes-14#fgs-types
        if (Version.sdkAboveOrEqual(Version.API34_ANDROID_14_UPSIDE_DOWN_CAKE)) {
            val fullScreenIntentPermission = Compatibility.hasFullScreenIntentPermission(
                this
            )
            Log.i(
                "[$TAG] Android 14 or above detected: full-screen intent permission is ${if (fullScreenIntentPermission) "granted" else "not granted"}"
            )
            if (!fullScreenIntentPermission) {
                Compatibility.requestFullScreenIntentPermission(this)
            }
        }
    }

    private fun checkTelecomManagerPermissions() {
        if (!corePreferences.useTelecomManager) {
            Log.i("[$TAG] Telecom Manager feature is disabled")
            if (corePreferences.manuallyDisabledTelecomManager) {
                Log.w("[$TAG] User has manually disabled Telecom Manager feature")
            } else {
                if (Compatibility.hasTelecomManagerPermissions(this)) {
                    enableTelecomManager()
                } else {
                    Log.i("[$TAG] Asking for Telecom Manager permissions")
                    Compatibility.requestTelecomManagerPermissions(this, 1)
                }
            }
        } else {
            Log.i("[$TAG] Telecom Manager feature is already enabled")
        }
    }

    private fun enableTelecomManager() {
        Log.i("[$TAG] Telecom Manager permissions granted")
        if (!TelecomHelper.exists()) {
            Log.i("[$TAG] Creating Telecom Helper")
            if (Compatibility.hasTelecomManagerFeature(this)) {
                TelecomHelper.create(this)
            } else {
                Log.e(
                    "[$TAG] Telecom Helper can't be created, device doesn't support connection service!"
                )
                return
            }
        } else {
            Log.e("[$TAG] Telecom Manager was already created ?!")
        }
        corePreferences.useTelecomManager = true
    }
}