package cc.cans.canscloud.demoappinsdk.call

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import cc.cans.canscloud.demoappinsdk.R

class CallActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        // Make activity full screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        // Show CallFragment when the activity starts
        if (savedInstanceState == null) {
            val callFragment = CallFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, callFragment)
                .commit()
        }
    }
}
