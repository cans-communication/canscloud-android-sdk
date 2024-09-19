package cc.cans.canscloud.demoappinsdk

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import cc.cans.canscloud.demoappinsdk.CansApplication.Companion.coreContext
import cc.cans.canscloud.sdk.compatibility.Compatibility
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.demoappinsdk.databinding.ActivityMainBinding
import cc.cans.canscloud.sdk.telecom.TelecomHelper
import cc.cans.canscloud.sdk.utils.PermissionHelper
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
                android.util.Log.i("[MainActivity]","READ_PHONE_STATE permission has been granted")
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
                android.util.Log.i("[MainActivity]","Telecom Manager permission have been granted")
                enableTelecomManager()
            } else {
                android.util.Log.w("[MainActivity]","Telecom Manager permission have been denied (at least one of them)")
            }
        } else if (requestCode == 2) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                android.util.Log.i("[MainActivity]","POST_NOTIFICATIONS permission has been granted")
            }
            checkTelecomManagerPermissions()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun checkPermissions() {
        if (!PermissionHelper.get().hasReadPhoneStatePermission()) {
            android.util.Log.i("[$TAG]","Asking for READ_PHONE_STATE permission")
            requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), 0)
        } else if (!PermissionHelper.get().hasPostNotificationsPermission()) {
            // Don't check the following the previous permission is being asked
            android.util.Log.i("[$TAG]","Asking for POST_NOTIFICATIONS permission")
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
            android.util.Log.i("[$TAG]"," Android 14 or above detected: full-screen intent permission is ${if (fullScreenIntentPermission) "granted" else "not granted"}"
            )
            if (!fullScreenIntentPermission) {
                Compatibility.requestFullScreenIntentPermission(this)
            }
        }
    }

    private fun checkTelecomManagerPermissions() {
        if (!corePreferences.useTelecomManager) {
            android.util.Log.i("[$TAG]","Telecom Manager feature is disabled")
            if (corePreferences.manuallyDisabledTelecomManager) {
                android.util.Log.w("[$TAG]"," User has manually disabled Telecom Manager feature")
            } else {
                if (Compatibility.hasTelecomManagerPermissions(this)) {
                    enableTelecomManager()
                } else {
                    android.util.Log.i("[$TAG]"," Asking for Telecom Manager permissions")
                    Compatibility.requestTelecomManagerPermissions(this, 1)
                }
            }
        } else {
            android.util.Log.i("[$TAG]"," Telecom Manager feature is already enabled")
        }
    }

    private fun enableTelecomManager() {
        android.util.Log.i("[$TAG]"," Telecom Manager permissions granted")
        if (!TelecomHelper.exists()) {
            android.util.Log.i("[$TAG]"," Creating Telecom Helper")
            if (Compatibility.hasTelecomManagerFeature(this)) {
                TelecomHelper.create(this)
            } else {
                android.util.Log.e(
                    "[$TAG]"," Telecom Helper can't be created, device doesn't support connection service!"
                )
            }
        } else {
            android.util.Log.e("[$TAG]"," Telecom Manager was already created ?!")
        }
        corePreferences.useTelecomManager = true
    }
}