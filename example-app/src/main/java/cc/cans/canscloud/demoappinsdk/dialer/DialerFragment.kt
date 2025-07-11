package cc.cans.canscloud.demoappinsdk.dialer

import android.os.Bundle
import android.text.Editable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import cc.cans.canscloud.demoappinsdk.R
import cc.cans.canscloud.demoappinsdk.databinding.FragmentDialerBinding
import cc.cans.canscloud.demoappinsdk.viewmodel.SharedMainViewModel
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter
import cc.cans.canscloud.sdk.models.CansTransport
import cc.cans.canscloud.sdk.models.RegisterState
import org.linphone.core.RegistrationState


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
        sharedViewModel = ViewModelProvider(this)[SharedMainViewModel::class.java]

        sharedViewModel.missedCallsCount.observe(viewLifecycleOwner) {
            binding.misscall.text = "MissCall : $it"
        }

        sharedViewModel.register()

        sharedViewModel.statusRegister.observe(viewLifecycleOwner) {
            binding.registerStatus.text = getString(it)
            binding.registerUser.text = cansCenter().account
        }

        sharedViewModel.isRegister.observe(viewLifecycleOwner) {
            binding.buttonRegister.visibility = if (!it) View.VISIBLE else View.GONE
            binding.buttonUnregister.visibility = if (it) View.VISIBLE else View.GONE
        }

        binding.buttonCall.setOnClickListener {
            if (binding.editTextPhoneNumber.text.isNotEmpty()) {
                cansCenter().startCall(binding.editTextPhoneNumber.text.toString())
            } else {
                if (cansCenter().lastOutgoingCallLog != "") {
                    binding.editTextPhoneNumber.text = Editable.Factory.getInstance().newEditable(cansCenter().lastOutgoingCallLog)
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.start_call_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.buttonRegister.setOnClickListener {
            cansCenter().register(
                "1003",
                "p1003",
                "sitmms.cans.cc",
                "8446",
                CansTransport.UDP
            )
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