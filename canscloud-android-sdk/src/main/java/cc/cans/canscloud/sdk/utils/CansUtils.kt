package cc.cans.canscloud.sdk.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.telephony.TelephonyManager.NETWORK_TYPE_EDGE
import android.telephony.TelephonyManager.NETWORK_TYPE_GPRS
import android.telephony.TelephonyManager.NETWORK_TYPE_IDEN
import org.linphone.core.Address
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CansUtils {
    companion object {
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

//        fun getRecordingFilePathForAddress(address: Address): String {
//            val displayName = getDisplayName(address)
//            val dateFormat: DateFormat = SimpleDateFormat(
//                RECORDING_DATE_PATTERN,
//                Locale.getDefault(),
//            )
//            val fileName = "${displayName}_${dateFormat.format(Date())}.mkv"
//            return FileUtils.getFileStoragePath(fileName).absolutePath
//        }
    }
}