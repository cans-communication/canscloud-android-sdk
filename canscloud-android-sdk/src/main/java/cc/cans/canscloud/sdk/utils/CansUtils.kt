package cc.cans.canscloud.sdk.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.telephony.TelephonyManager.NETWORK_TYPE_EDGE
import android.telephony.TelephonyManager.NETWORK_TYPE_GPRS
import android.telephony.TelephonyManager.NETWORK_TYPE_IDEN
import androidx.annotation.Keep
import androidx.annotation.WorkerThread
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.sdk.CansCenter
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter
import org.linphone.core.Account
import org.linphone.core.Address
import org.linphone.core.Conference
import org.linphone.core.tools.Log

class CansUtils {
    @Keep
    companion object {
        private val cans: Cans = CansCenter()
        private const val TAG = "[Cans Utils]"

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

        @WorkerThread
        fun getDisplayableAddress(address: Address?): String {
            if (address == null) return "[null]"
            return if (cansCenter().corePreferences.replaceSipUriByUsername) {
                address.username ?: address.asStringUriOnly()
            } else {
                val copy = address.clone()
                copy.clean() // To remove gruu if any
                copy.asStringUriOnly()
            }
        }

        @WorkerThread
        fun getDefaultAccount(): Account? {
            return cansCenter().core.defaultAccount ?: cansCenter().core.accountList.firstOrNull()
        }

        @WorkerThread
        fun createGroupCall(account: Account?, subject: String): Conference? {
            val core = cansCenter().core
            val conferenceParams = core.createConferenceParams(null)
            conferenceParams.isVideoEnabled = true
            conferenceParams.account = account
            conferenceParams.subject = subject

            // Enable end-to-end encryption if client supports it
            conferenceParams.securityLevel = if (cansCenter().corePreferences.createEndToEndEncryptedMeetingsAndGroupCalls) {
                Log.i("$TAG Requesting EndToEnd security level for conference")
                Conference.SecurityLevel.EndToEnd
            } else {
                Log.i("$TAG Requesting PointToPoint security level for conference")
                Conference.SecurityLevel.PointToPoint
            }

            // Allows to have a chat room within the conference
            conferenceParams.isChatEnabled = true

            Log.i("$TAG Creating group call with subject ${conferenceParams.subject}")
            return core.createConferenceWithParams(conferenceParams)
        }
    }
}