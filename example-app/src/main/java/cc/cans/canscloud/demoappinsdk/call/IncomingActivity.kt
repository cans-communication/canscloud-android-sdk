package cc.cans.canscloud.demoappinsdk.call

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import cc.cans.canscloud.demoappinsdk.databinding.ActivityIncomingBinding
import cc.cans.canscloud.demoappinsdk.viewmodel.CallsViewModel
import cc.cans.canscloud.sdk.compatibility.Compatibility
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter
import cc.cans.canscloud.sdk.utils.PermissionHelper

class IncomingActivity : AppCompatActivity() {
    val TAG = "IncomingActivity"
    private lateinit var binding: ActivityIncomingBinding
    private lateinit var callsViewModel: CallsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncomingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        callsViewModel = this.run {
            ViewModelProvider(this)[CallsViewModel::class.java]
        }

        binding.contactName.text = cansCenter().destinationUsername

        callsViewModel.isCallEnd.observe(this) {
            finish()
        }

        binding.acceptCall.setOnClickListener {
            cansCenter().startAnswerCall()
            finish()
        }

        binding.hangUp.setOnClickListener {
            cansCenter().terminateCall()
            finish()
        }

        checkPermissions()
    }

    private fun checkPermissions() {
        val permissionsRequiredList = arrayListOf<String>()

        if (!PermissionHelper.singletonHolder().get().hasRecordAudioPermission()) {
            android.util.Log.i("[$TAG]","Asking for RECORD_AUDIO permission")
            permissionsRequiredList.add(Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT >= (Build.VERSION_CODES.S) && !PermissionHelper.singletonHolder().get().hasBluetoothConnectPermission()) {
            android.util.Log.i("[$TAG]","Asking for BLUETOOTH_CONNECT permission")
            permissionsRequiredList.add(Compatibility.BLUETOOTH_CONNECT)
        }

        if (permissionsRequiredList.isNotEmpty()) {
            val permissionsRequired = arrayOfNulls<String>(permissionsRequiredList.size)
            permissionsRequiredList.toArray(permissionsRequired)
            requestPermissions(permissionsRequired, 0)
        }
    }
}
