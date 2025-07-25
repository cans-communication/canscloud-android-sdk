package cc.cans.canscloud.sdk

import android.app.Activity
import android.content.Context
import android.os.Vibrator
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.sdk.core.CoreContextSDK
import cc.cans.canscloud.sdk.core.CorePreferences
import cc.cans.canscloud.sdk.core.CoreService
import cc.cans.canscloud.sdk.data.GroupedCallLogData
import cc.cans.canscloud.sdk.models.CallModel
import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.models.CansTransport
import cc.cans.canscloud.sdk.models.RegisterState
import cc.cans.canscloud.sdk.okta.models.SignInOKTAResponseData
import org.linphone.core.Call
import org.linphone.core.Conference
import org.linphone.core.Core

interface Cans {

    var core: Core

    var callCans: Call

    var mVibrator: Vibrator

    val callState: CallState

    var corePreferences: CorePreferences

    var context: Context

    var conferenceCore: Conference

    var coreContext: CoreContextSDK

    var coreService: CoreService

    var appName: String

    val account: String

    val username: String

    val domain: String

    val port: String

    val proxy: String

    val defaultStateRegister: RegisterState

    val destinationRemoteAddress: String

    val destinationUsername: String

    val lastOutgoingCallLog: String

    val durationTime: Int?

    val startDateCall: Int

    val missedCallsCount: Int

    val countCalls: Int

    val isBluetoothDevices : Boolean

    val isMicState: Boolean

    val isSpeakerState: Boolean

    val isBluetoothState: Boolean

    val isBluetoothAudioRouteAvailable: Boolean

    val isHeadsetState: Boolean

    val wasBluetoothPreviouslyAvailable: Boolean

    val callLogs: ArrayList<GroupedCallLogData>

    val missedCallLogs:ArrayList<GroupedCallLogData>

    val isInConference: Boolean

    val isMeConferenceFocus : Boolean

    fun config(context: Context, appName: String)

    fun register(
        username: String,
        password: String,
        domain: String,
        port: String,
        transport: CansTransport
    )

    fun refreshRegister()

    fun getCallLog(): ArrayList<CallModel>

    fun registerAccount(username: String, password: String, domain: String)

    fun requestPermissionPhone(activity: Activity)

    fun requestPermissionAudio(activity: Activity)

    fun enableTelecomManager(activity: Activity)

    fun checkTelecomManagerPermissions(activity: Activity)

    fun removeAccountAll()

    fun removeAccount(index: Int, username: String, domain: String)

    fun startCall(addressToCall: String)

    fun terminateCall()

    fun pause(index: Int, addressToCall: String)

    fun resume(index: Int, addressToCall: String)

    fun terminate(index: Int, addressToCall: String)

    fun terminateAllCalls()

    fun startAnswerCall()

    fun accountList(): ArrayList<String>

    fun defaultAccount(index: Int, phoneNumber: String)

    fun updateAudioRelated()

    fun updateAudioRoutesState()

    fun toggleSpeaker()

    fun toggleMuteMicrophone()

    fun forceEarpieceAudioRoute()

    fun routeAudioToBluetooth()

    fun routeAudioToHeadset()

    fun routeAudioToSpeaker()

    fun audioDevicesListUpdated()

    fun isCallLogMissed(): Boolean

    fun isPauseState() : Boolean

    fun isOutgoingState() : Boolean

    fun updateCallLogs()

    fun updateMissedCallLogs()

    fun addListener(listener: CansListenerStub)

    fun removeListener(listener: CansListenerStub)

    fun removeAllListener()

    fun transferNow(phoneNumber: String) : Boolean

    fun askFirst(phoneNumber: String) : Boolean

    fun dtmfKey(key: String)

    fun mergeCallsIntoConference()

    fun signInOKTADomain(apiURL: String, domain: String, activity: Activity, onResult: (Int) -> Unit)

    fun signOutOKTADomain(activity: Activity,callback: (Boolean) -> Unit)

    fun isSignInOKTANotConnected() : Boolean

    fun checkSessionOKTAExpire(activity: Activity,callback: (Boolean) -> Unit)

    fun fetchSignInOKTA(apiURL: String, callback: (SignInOKTAResponseData?) -> Unit)
}