package cc.cans.canscloud.sdk

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.ui.AppBarConfiguration
import cc.cans.canscloud.sdk.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Cans.config(this, packageManager, packageName, "robinhood") {
            binding.register.text = Cans.username()
        }

        binding.buttonCall.setOnClickListener {
            Cans.startCall("0957414609")
        }
    }
}