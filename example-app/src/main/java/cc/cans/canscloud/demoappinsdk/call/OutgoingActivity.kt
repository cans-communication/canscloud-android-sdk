package cc.cans.canscloud.demoappinsdk.call

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import cc.cans.canscloud.demoappinsdk.R
import cc.cans.canscloud.demoappinsdk.databinding.ActivityOutgoinglBinding
import cc.cans.canscloud.demoappinsdk.viewmodel.OutgoingViewModel
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.sdk.Cans.Companion

class OutgoingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOutgoinglBinding
    private lateinit var outgoingViewModel: OutgoingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOutgoinglBinding.inflate(layoutInflater)
        setContentView(binding.root)

        outgoingViewModel = ViewModelProvider(this)[OutgoingViewModel::class.java]

        binding.textViewPhoneNumber.text = Cans.destinationUsername

        outgoingViewModel.isCallEnd.observe(this) {
            finish()
        }

        binding.buttonHangUp.setOnClickListener {
            Cans.terminateCall()
            finish()
        }

        binding.micro.setOnClickListener {
            Cans.toggleMuteMicrophone()
            if (Cans.isMicState) {
                binding.micro.setImageResource(R.drawable.ongoing_mute_select)
            } else {
                binding.micro.setImageResource(R.drawable.ongoing_mute_default)
            }
        }

        binding.speaker.setOnClickListener {
            Cans.toggleSpeaker()
            if (Cans.isSpeakerState) {
                binding.speaker.setImageResource(R.drawable.ongoing_speaker_selected)
            } else {
                binding.speaker.setImageResource(R.drawable.ongoing_speaker_default)
            }
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
