package cc.cans.canscloud.demoappinsdk.call

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import cc.cans.canscloud.demoappinsdk.R
import cc.cans.canscloud.demoappinsdk.databinding.FragmentCallBinding
import cc.cans.canscloud.demoappinsdk.viewmodel.CallsViewModel
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter

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

        binding.textViewPhoneNumber.text = cansCenter().destinationUsername


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
                binding.speaker.visibility = View.GONE
            } else {
                binding.bluetooth.visibility = View.GONE
                binding.speaker.visibility = View.VISIBLE
            }
        }

        callsViewModel.isSpeaker.observe(viewLifecycleOwner) {
            if (it) {
                binding.speaker.setImageResource(R.drawable.ongoing_speaker_selected)
            } else {
                binding.speaker.setImageResource(R.drawable.ongoing_speaker_default)
            }
        }

        binding.buttonCall.setOnClickListener {
            cansCenter().startCall("0838927729")
            Log.i("callingLogs........: ","${cansCenter().callingLogs.size}")
        }

        binding.buttonHangUp.setOnClickListener {
            cansCenter().terminateCall()
            requireActivity().finish()
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
        }

        binding.bluetooth.setOnClickListener {
            cansCenter().routeAudioToBluetooth()
        }

        cansCenter().requestPermissionAudio(requireActivity())
    }
}