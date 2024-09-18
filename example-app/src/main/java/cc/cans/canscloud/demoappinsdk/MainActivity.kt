package cc.cans.canscloud.demoappinsdk

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import cc.cans.canscloud.demoappinsdk.compatibility.Compatibility
import cc.cans.canscloud.demoappinsdk.core.CoreContext
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.demoappinsdk.databinding.ActivityMainBinding
import cc.cans.canscloud.demoappinsdk.notifaication.NotificationsManager
import cc.cans.canscloud.sdk.Cans.Companion.corePreferences
import cc.cans.canscloud.sdk.models.CansTransport

class MainActivity : AppCompatActivity() {
    private val POST_NOTIFICATIONS_REQUEST_CODE = 1001
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

        val permissionsRequiredList = arrayListOf<String>()
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            permissionsRequiredList.add(Compatibility.BLUETOOTH_CONNECT)
        }

        if (checkSelfPermission(Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED) {
            permissionsRequiredList.add(Manifest.permission.READ_PHONE_NUMBERS)
        }

        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsRequiredList.add(Manifest.permission.READ_PHONE_STATE)
        }

        if (checkSelfPermission(Manifest.permission.MANAGE_OWN_CALLS) != PackageManager.PERMISSION_GRANTED) {
            permissionsRequiredList.add(Manifest.permission.MANAGE_OWN_CALLS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsRequiredList.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsRequiredList.isNotEmpty()) {
            val permissionsRequired = arrayOfNulls<String>(permissionsRequiredList.size)
            permissionsRequiredList.toArray(permissionsRequired)
            requestPermissions(permissionsRequired, 0)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == POST_NOTIFICATIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with showing notifications
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
            } else {
                // Permission denied
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}