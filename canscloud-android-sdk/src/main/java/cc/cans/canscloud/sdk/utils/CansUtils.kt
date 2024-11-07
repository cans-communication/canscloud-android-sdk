package cc.cans.canscloud.sdk.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.telephony.TelephonyManager.NETWORK_TYPE_EDGE
import android.telephony.TelephonyManager.NETWORK_TYPE_GPRS
import android.telephony.TelephonyManager.NETWORK_TYPE_IDEN
import androidx.annotation.Keep
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.sdk.CansCenter
import org.linphone.core.Address

class CansUtils {
    @Keep
    companion object {
        private val cans: Cans = CansCenter()

        @SuppressLint("MissingPermission")
        fun checkIfNetworkHasLowBandwidth(context: Context): Boolean {
            val connMgr =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo: NetworkInfo? = connMgr.activeNetworkInfo
            if (networkInfo?.isConnected == true && networkInfo.type == ConnectivityManager.TYPE_MOBILE) {
                return when (networkInfo.subtype) {
                    NETWORK_TYPE_EDGE, NETWORK_TYPE_GPRS, NETWORK_TYPE_IDEN -> true
                    else -> false
                }
            }
            // In doubt return false
            return false
        }

        fun getDisplayableAddress(address: Address?): String {
            if (address == null) return "[null]"
            return if (cans.corePreferences.replaceSipUriByUsername) {
                address.username ?: address.asStringUriOnly()
            } else {
                val copy = address.clone()
                copy.clean() // To remove gruu if any
                copy.asStringUriOnly()
            }
        }
    }
}