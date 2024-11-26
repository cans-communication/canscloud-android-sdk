package cc.cans.canscloud.demoappinsdk.call

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import cc.cans.canscloud.demoappinsdk.R
import cc.cans.canscloud.demoappinsdk.databinding.ActivityOutgoinglBinding
import cc.cans.canscloud.demoappinsdk.viewmodel.OutgoingViewModel
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter

class OutgoingActivity : AppCompatActivity() {
    val TAG = "OutgoingActivity"
    private lateinit var binding: ActivityOutgoinglBinding
    private lateinit var outgoingViewModel: OutgoingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOutgoinglBinding.inflate(layoutInflater)
        setContentView(binding.root)

        outgoingViewModel = ViewModelProvider(this)[OutgoingViewModel::class.java]

        binding.textViewPhoneNumber.text = cansCenter().destinationUsername

        outgoingViewModel.isCallEnd.observe(this) {
            finish()
        }

        binding.buttonHangUp.setOnClickListener {
            cansCenter().terminateCall()
            finish()
        }

        binding.micro.setOnClickListener {
            cansCenter().toggleMuteMicrophone()
            if (cansCenter().isMicState) {
                binding.micro.setImageResource(R.drawable.ongoing_mute_select)
            } else {
                binding.micro.setImageResource(R.drawable.ongoing_mute_default)
            }
        }

        binding.speaker.setOnClickListener {
            cansCenter().toggleSpeaker()
            if (cansCenter().isSpeakerState) {
                binding.speaker.setImageResource(R.drawable.ongoing_speaker_selected)
            } else {
                binding.speaker.setImageResource(R.drawable.ongoing_speaker_default)
            }
        }

        cansCenter().requestPermissionAudio(this)
    }
}
