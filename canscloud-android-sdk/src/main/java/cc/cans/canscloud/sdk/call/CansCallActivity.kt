package cc.cans.canscloud.sdk.call

import android.os.Bundle
import androidx.activity.ComponentActivity
import cc.cans.canscloud.sdk.databinding.ActivityCansCallBinding

class CansCallActivity : ComponentActivity() {

    private lateinit var binding: ActivityCansCallBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCansCallBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

}