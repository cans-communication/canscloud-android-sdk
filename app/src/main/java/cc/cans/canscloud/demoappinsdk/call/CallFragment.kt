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
import cc.cans.canscloud.demoappinsdk.viewmodel.SharedMainViewModel
import cc.cans.canscloud.sdk.Cans

/**
 * A simple [Fragment] subclass.
 * Use the [CallFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CallFragment : Fragment() {

    private lateinit var callsViewModel: CallsViewModel
    private lateinit var sharedViewModel: SharedMainViewModel
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

        callsViewModel = ViewModelProvider(this)[CallsViewModel::class.java]

        sharedViewModel = ViewModelProvider(this)[SharedMainViewModel::class.java]


        callsViewModel = requireActivity().run {
            ViewModelProvider(this)[CallsViewModel::class.java]
        }

        callsViewModel.isCallEnd.observe(viewLifecycleOwner) {
            requireActivity().finish()
        }

        val phoneNumber = arguments?.getString("phoneNumber")
        binding.textViewPhoneNumber.text = Cans.usernameCall()
        Cans.updateMicState()
        Cans.updateSpeakerState()

        sharedViewModel.callDuration.observe(viewLifecycleOwner) {
            binding.activeCallTimer.visibility = View.VISIBLE
            binding.activeCallTimer.base = SystemClock.elapsedRealtime() - (1000 * Cans.durationTime()!!) // Linphone timestamps are in seconds
            binding.activeCallTimer.start()
        }

        binding.buttonHangUp.setOnClickListener {
            Cans.terminateCall()
            requireActivity().finish()
        }

        binding.micro.setOnClickListener {
            Cans.toggleMuteMicrophone()
            if (Cans.isMicrophoneMuted) {
                binding.micro.setImageResource(R.drawable.ongoing_mute_select)
            } else {
                binding.micro.setImageResource(R.drawable.ongoing_mute_default)
            }
        }

        binding.speaker.setOnClickListener {
            Cans.toggleSpeaker()
            if (Cans.isSpeakerSelected) {
                binding.speaker.setImageResource(R.drawable.ongoing_speaker_selected)
            } else {
                binding.speaker.setImageResource(R.drawable.ongoing_speaker_default)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
    }

}