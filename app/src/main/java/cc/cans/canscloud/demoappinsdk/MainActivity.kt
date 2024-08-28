package cc.cans.canscloud.demoappinsdk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.findNavController
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.demoappinsdk.databinding.ActivityMainBinding
import cc.cans.canscloud.sdk.call.CansCallActivity
import cc.cans.canscloud.sdk.callback.CallCallback
import cc.cans.canscloud.sdk.callback.RegisterCallback

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val coreListener = object : CallCallback {
        override fun onCallOutGoing() {
            Log.i("Cans Center","onCallOutGoing")
        }

        override fun onCallEnd() {
            Log.i("Cans Center","onCallEnd")
        }

        override fun onStartCall() {
            Log.i("Cans Center","onStartCall")
        }

        override fun onConnected() {
            Log.i("Cans Center","onConnected")
        }

        override fun onError(message: String) {
            Log.i("Cans Center","onError")
        }

        override fun onLastCallEnd() {
            Log.i("Cans Center","onLastCallEnd")
        }

        override fun onInComingCall() {
            Log.i("Cans Center","onInComingCall")
        }
    }


    private val registerListener = object : RegisterCallback {
        override fun onRegistrationOk() {
            Log.i("Cans Center","onRegistrationOk")
        }

        override fun onRegistrationFail(message: String) {
            Log.i("Cans Center","onRegistrationFail")
        }

        override fun onUnRegister() {
            Log.i("Cans Center","onUnRegister")
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Cans.registerCallListener(coreListener)
        Cans.registersListener(registerListener)
        Cans.config(this, packageManager, packageName) {
            Cans.register(this,"line")
            //Cans.registerByUser(this, "40102", "p40102CANS","cns.cans.cc","8446", "udp" )
            binding.register.text = Cans.username()
        }
        
        binding.buttonCall.setOnClickListener {
            val intent = Intent(this, CansCallActivity::class.java)
            intent.putExtra("phoneNumber", binding.editTextPhoneNumber.text.toString())
            startActivity(intent)
        }

        binding.buttonRegister.setOnClickListener {
            Cans.register(this,"line")
            Cans.registerCallListener(coreListener)
            Cans.registersListener(registerListener)
            binding.register.text = Cans.username()
        }

        binding.buttonUnregister.setOnClickListener {
            Cans.unCallListener(coreListener)
            Cans.unRegisterListener(registerListener)
            binding.register.text = Cans.username()
        }
    }
}