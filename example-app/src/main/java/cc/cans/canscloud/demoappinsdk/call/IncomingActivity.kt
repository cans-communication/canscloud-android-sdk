package cc.cans.canscloud.demoappinsdk.call

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import cc.cans.canscloud.demoappinsdk.databinding.ActivityIncomingBinding
import cc.cans.canscloud.demoappinsdk.viewmodel.CallsViewModel
import cc.cans.canscloud.sdk.Cans

class IncomingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncomingBinding
    private lateinit var callsViewModel: CallsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncomingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        callsViewModel = this.run {
            ViewModelProvider(this)[CallsViewModel::class.java]
        }

        binding.contactName.text = Cans.destinationUsername

        callsViewModel.isCallEnd.observe(this) {
            finish()
        }

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
