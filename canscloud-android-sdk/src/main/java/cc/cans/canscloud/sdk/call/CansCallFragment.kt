package cc.cans.canscloud.sdk.call

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cc.cans.canscloud.sdk.R


/**
 * A simple [Fragment] subclass.
 * Use the [CansCallFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CansCallFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_cans_call, container, false)
    }
}