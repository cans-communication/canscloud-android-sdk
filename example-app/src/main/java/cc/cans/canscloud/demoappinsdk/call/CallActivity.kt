package cc.cans.canscloud.demoappinsdk.call

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cc.cans.canscloud.demoappinsdk.R

class CallActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        // Show CallFragment when the activity starts
        if (savedInstanceState == null) {
            val callFragment = CallFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, callFragment)
                .commit()
        }

        if (packageManager?.checkPermission(
                Manifest.permission.RECORD_AUDIO,
                packageName
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            this.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 0)
            return
        }
    }
}
