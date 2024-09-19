package cc.cans.canscloud.demoappinsdk.call

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import cc.cans.canscloud.demoappinsdk.R
import cc.cans.canscloud.demoappinsdk.databinding.FragmentCallBinding
import cc.cans.canscloud.sdk.utils.AudioRouteUtils
import cc.cans.canscloud.demoappinsdk.viewmodel.CallsViewModel
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.sdk.compatibility.Compatibility
import cc.cans.canscloud.sdk.utils.PermissionHelper
import org.linphone.core.tools.Log

/**
 * A simple [Fragment] subclass.
 * Use the [CallFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CallFragment : Fragment() {
    val TAG = "CallFragment"
    private lateinit var callsViewModel: CallsViewModel
    private var _binding: FragmentCallBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        callsViewModel = requireActivity().run {
            ViewModelProvider(this)[CallsViewModel::class.java]
        }

        binding.textViewPhoneNumber.text = Cans.destinationUsername

        if (AudioRouteUtils.isBluetoothAudioRouteAvailable()) {
            binding.bluetooth.visibility = View.VISIBLE
        } else {
            binding.bluetooth.visibility = View.GONE
        }

        callsViewModel.isCallEnd.observe(viewLifecycleOwner) {
            requireActivity().finish()
        }

        callsViewModel.callDuration.observe(viewLifecycleOwner) { duration ->
            if (duration != null) {
                binding.activeCallTimer.visibility = View.VISIBLE
                binding.activeCallTimer.base =
                    SystemClock.elapsedRealtime() - (1000 * duration)
                binding.activeCallTimer.start()
            }
        }

        callsViewModel.isBluetooth.observe(viewLifecycleOwner) {
            if (it) {
                binding.bluetooth.visibility = View.VISIBLE
            } else {
                binding.bluetooth.visibility = View.GONE
            }
        }

        binding.buttonHangUp.setOnClickListener {
            Cans.terminateCall()
            requireActivity().finish()
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

        binding.bluetooth.setOnClickListener {
            Cans.forceBluetoothAudioRoute()
        }

        checkPermissions()
    }

    private fun checkPermissions() {
        val permissionsRequiredList = arrayListOf<String>()

        if (!PermissionHelper.get().hasRecordAudioPermission()) {
            Log.i("[$TAG]","Asking for RECORD_AUDIO permission")
            permissionsRequiredList.add(Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT >= (Build.VERSION_CODES.S) && !PermissionHelper.get().hasBluetoothConnectPermission()) {
            Log.i("[$TAG]","Asking for BLUETOOTH_CONNECT permission")
            permissionsRequiredList.add(Compatibility.BLUETOOTH_CONNECT)
        }

        if (permissionsRequiredList.isNotEmpty()) {
            val permissionsRequired = arrayOfNulls<String>(permissionsRequiredList.size)
            permissionsRequiredList.toArray(permissionsRequired)
            requestPermissions(permissionsRequired, 0)
        }
    }
}