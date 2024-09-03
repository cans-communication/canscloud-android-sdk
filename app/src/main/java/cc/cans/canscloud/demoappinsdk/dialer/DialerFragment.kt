package cc.cans.canscloud.demoappinsdk.dialer

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import cc.cans.canscloud.demoappinsdk.call.CallActivity
import cc.cans.canscloud.demoappinsdk.databinding.FragmentDialerBinding
import cc.cans.canscloud.demoappinsdk.notifaication.NotificationsApp
import cc.cans.canscloud.demoappinsdk.viewmodel.SharedMainViewModel
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.sdk.call.CansCallActivity
import cc.cans.canscloud.sdk.models.CansTransportType


/**
 * A simple [Fragment] subclass.
 * Use the [DialerFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DialerFragment : Fragment() {

    private var _binding: FragmentDialerBinding? = null
    private lateinit var sharedViewModel: SharedMainViewModel

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDialerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // binding.textViewUsername.text = "Register by : " + Cans.username()
        sharedViewModel = ViewModelProvider(this)[SharedMainViewModel::class.java]

        sharedViewModel.missedCallsCount.observe(viewLifecycleOwner) {
            binding.misscall.text = "MissCall : $it"
        }

        sharedViewModel.statusRegister.observe(viewLifecycleOwner) {
            binding.registerStatus.text = getString(it)
            binding.registerUser.text = Cans.accountRegister
        }

        binding.buttonCall.setOnClickListener {
            Cans.startCall(binding.editTextPhoneNumber.text.toString())
            val intent = Intent(requireContext(), CallActivity::class.java)
            intent.putExtra("phoneNumber", binding.editTextPhoneNumber.text.toString())
            startActivity(intent)
        }

        binding.buttonRegister.setOnClickListener {
            Cans.register(requireActivity(), "line")
            sharedViewModel.register()
        }

        binding.buttonUnregister.setOnClickListener {
            sharedViewModel.unregister()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}