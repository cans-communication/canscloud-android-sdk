package cc.cans.canscloud.demoappinsdk.call

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import cc.cans.canscloud.demoappinsdk.databinding.ActivityIncomingBinding
import cc.cans.canscloud.demoappinsdk.viewmodel.CallsViewModel
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter

class IncomingActivity : AppCompatActivity() {
    val TAG = "IncomingActivity"
    private lateinit var binding: ActivityIncomingBinding
    private lateinit var callsViewModel: CallsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncomingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        callsViewModel = this.run {
            ViewModelProvider(this)[CallsViewModel::class.java]
        }

        binding.contactName.text = cansCenter().destinationUsername

        callsViewModel.isCallEnd.observe(this) {
            finish()
        }

        binding.acceptCall.setOnClickListener {
            cansCenter().startAnswerCall()
            finish()
        }

        binding.hangUp.setOnClickListener {
            cansCenter().terminateCall()
            finish()
        }

        cansCenter().requestPermissionAudio(this)
    }
}
