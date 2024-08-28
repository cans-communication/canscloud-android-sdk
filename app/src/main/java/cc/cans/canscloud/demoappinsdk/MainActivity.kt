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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val coreListener = object : CallCallback {

        override fun onStartCall() {
            Log.i("Cans Center","onStartCall")
        }

        override fun onConnected() {
            Log.i("Cans Center","onConnected")
        }

        override fun onError(message: String) {
            Log.i("Cans Center","onError")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Cans.registerCallListener(coreListener)
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
    }
}