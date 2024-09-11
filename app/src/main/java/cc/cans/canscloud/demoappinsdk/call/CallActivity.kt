package cc.cans.canscloud.demoappinsdk.call

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
    }
}
