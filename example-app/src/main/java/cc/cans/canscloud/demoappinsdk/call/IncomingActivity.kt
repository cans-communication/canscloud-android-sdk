package cc.cans.canscloud.demoappinsdk.call

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import cc.cans.canscloud.demoappinsdk.databinding.ActivityIncomingBinding
import cc.cans.canscloud.demoappinsdk.utils.PermissionHelper
import cc.cans.canscloud.demoappinsdk.viewmodel.CallsViewModel
import cc.cans.canscloud.sdk.Cans
import org.linphone.core.tools.Log
import org.linphone.mediastream.Version
import cc.cans.canscloud.demoappinsdk.compatibility.Compatibility

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

        val permissionsRequiredList = arrayListOf<String>()
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            permissionsRequiredList.add(Compatibility.BLUETOOTH_CONNECT)
        }

        if (checkSelfPermission(Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED) {
            permissionsRequiredList.add(Manifest.permission.READ_PHONE_NUMBERS)
        }

        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsRequiredList.add(Manifest.permission.READ_PHONE_STATE)
        }

        if (checkSelfPermission(Manifest.permission.MANAGE_OWN_CALLS) != PackageManager.PERMISSION_GRANTED) {
            permissionsRequiredList.add(Manifest.permission.MANAGE_OWN_CALLS)
        }

        if (permissionsRequiredList.isNotEmpty()) {
            val permissionsRequired = arrayOfNulls<String>(permissionsRequiredList.size)
            permissionsRequiredList.toArray(permissionsRequired)
            requestPermissions(permissionsRequired, 0)
        }

    }
}
