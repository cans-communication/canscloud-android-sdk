package cc.cans.canscloud.demoappinsdk

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.findNavController
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.demoappinsdk.databinding.ActivityMainBinding
import cc.cans.canscloud.sdk.CansApi
import cc.cans.canscloud.sdk.call.CansCallActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    val cansApi: CansApi = Cans.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cansApi.config(this, packageManager, packageName, "robinhood") {
            binding.register.text = cansApi.username()
        }

        binding.buttonCall.setOnClickListener {
            val intent = Intent(this, CansCallActivity::class.java)
            intent.putExtra("phoneNumber", "50103")
            startActivity(intent)
        }
    }
}