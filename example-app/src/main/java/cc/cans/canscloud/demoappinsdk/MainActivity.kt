package cc.cans.canscloud.demoappinsdk

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import cc.cans.canscloud.demoappinsdk.CansApplication.Companion.coreContext
import cc.cans.canscloud.demoappinsdk.databinding.ActivityMainBinding
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter
import cc.cans.canscloud.sdk.models.CansTransport

class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cansCenter().register(
            "40107",
            "p40107CANS",
            "cns.cans.cc",
            "8446",
            CansTransport.TCP
        )

        cansCenter().requestPermissionPhone(this)
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
                cansCenter().requestPermissionPhone(this)
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
                cansCenter().enableTelecomManager(this)
            } else {
                android.util.Log.w("[MainActivity]","Telecom Manager permission have been denied (at least one of them)")
            }
        } else if (requestCode == 2) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                android.util.Log.i("[MainActivity]","POST_NOTIFICATIONS permission has been granted")
            }
            cansCenter().checkTelecomManagerPermissions(this)
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}