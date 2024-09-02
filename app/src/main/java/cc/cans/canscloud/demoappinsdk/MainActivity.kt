package cc.cans.canscloud.demoappinsdk

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.findNavController
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.demoappinsdk.databinding.ActivityMainBinding
import cc.cans.canscloud.demoappinsdk.notifaication.NotificationsApp
import cc.cans.canscloud.sdk.call.CansCallActivity
import cc.cans.canscloud.sdk.callback.CallCallback
import cc.cans.canscloud.sdk.callback.RegisterCallback

class MainActivity : AppCompatActivity() {
    private val POST_NOTIFICATIONS_REQUEST_CODE = 1001
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

        createNotificationChannel()
        Cans.config(this, packageManager, packageName) {
           // Cans.register(this,"line")
            Cans.registerByUser(this, "40107", "p40107CANS","cns.cans.cc","8446", "udp" )
            binding.register.text = Cans.accountRegister
            NotificationsApp.onCoreReady()
            Cans.registerCallListener(coreListener)
            Cans.registersListener(registerListener)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Request permission
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), POST_NOTIFICATIONS_REQUEST_CODE)
            } else {
                // Permission already granted, proceed with showing notifications
            }
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
            binding.register.text = Cans.accountRegister
        }

        binding.buttonUnregister.setOnClickListener {
            Cans.unCallListener(coreListener)
            Cans.unRegisterListener(registerListener)
            binding.register.text = Cans.accountRegister
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "incoming_call_channel"
            val channelName = "Incoming Call"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance)
            channel.description = "Channel for incoming call notifications"
            channel.setSound(null, null) // Disable sound if required

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == POST_NOTIFICATIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with showing notifications
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
            } else {
                // Permission denied
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}