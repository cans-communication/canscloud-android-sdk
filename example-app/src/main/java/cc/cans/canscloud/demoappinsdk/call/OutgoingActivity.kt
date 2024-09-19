package cc.cans.canscloud.demoappinsdk.call

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import cc.cans.canscloud.demoappinsdk.R
import cc.cans.canscloud.demoappinsdk.databinding.ActivityOutgoinglBinding
import cc.cans.canscloud.demoappinsdk.viewmodel.OutgoingViewModel
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.sdk.compatibility.Compatibility
import org.linphone.core.tools.Log
import org.linphone.mediastream.Version
import cc.cans.canscloud.sdk.utils.PermissionHelper

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
            outgoingViewModel.toggleMuteMicrophone()
            if (Cans.isMicState) {
                binding.micro.setImageResource(R.drawable.ongoing_mute_select)
            } else {
                binding.micro.setImageResource(R.drawable.ongoing_mute_default)
            }
        }

        binding.speaker.setOnClickListener {
            outgoingViewModel.toggleSpeaker()
            if (Cans.isSpeakerState) {
                binding.speaker.setImageResource(R.drawable.ongoing_speaker_selected)
            } else {
                binding.speaker.setImageResource(R.drawable.ongoing_speaker_default)
            }
        }

        checkPermissions()
    }

    private fun checkPermissions() {
        val permissionsRequiredList = arrayListOf<String>()

        if (!PermissionHelper.get().hasRecordAudioPermission()) {
            Log.i("[OutgoingActivity] Asking for RECORD_AUDIO permission")
            permissionsRequiredList.add(Manifest.permission.RECORD_AUDIO)
        }

        if (Version.sdkAboveOrEqual(Version.API31_ANDROID_12) && !PermissionHelper.get().hasBluetoothConnectPermission()) {
            Log.i("[OutgoingActivity] Asking for BLUETOOTH_CONNECT permission")
            permissionsRequiredList.add(Compatibility.BLUETOOTH_CONNECT)
        }

        if (permissionsRequiredList.isNotEmpty()) {
            val permissionsRequired = arrayOfNulls<String>(permissionsRequiredList.size)
            permissionsRequiredList.toArray(permissionsRequired)
            requestPermissions(permissionsRequired, 0)
        }
    }
}
