package cc.cans.canscloud.demoappinsdk.call

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import cc.cans.canscloud.demoappinsdk.databinding.ActivityIncomingBinding
import cc.cans.canscloud.sdk.Cans

class IncomingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncomingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncomingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Make activity full screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        binding.contactName.text = Cans.usernameCall()

        binding.acceptCall.setOnClickListener {
            Cans.startAnswerCall()
            finish()
        }

        binding.hangUp.setOnClickListener {
            Cans.terminateCall()
            finish()
        }

    }

}
