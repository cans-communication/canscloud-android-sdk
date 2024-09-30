package cc.cans.canscloud.sdk

import android.content.Context
import android.os.Vibrator
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.sdk.core.CorePreferences
import cc.cans.canscloud.sdk.models.CansTransport
import org.linphone.core.Call
import org.linphone.core.Core

interface Cans {

    var core: Core

    var callCans: Call

    var mVibrator: Vibrator

    var corePreferences: CorePreferences

    var context: Context

    val account: String

    val username: String

    val domain: String

    val port: String

    val destinationRemoteAddress: String

    val destinationUsername: String

    val durationTime: Int?

    val missedCallsCount: Int

    val countCalls: Int

    val isMicState: Boolean

    val isSpeakerState: Boolean

    val isBluetoothState: Boolean

    fun config(context: Context, appName: String)

    fun register(
        username: String,
        password: String,
        domain: String,
        port: String,
        transport: CansTransport
    )

    fun removeAccount()

    fun startCall(addressToCall: String)

    fun terminateCall()

    fun startAnswerCall()

    fun toggleSpeaker()

    fun toggleMuteMicrophone()

    fun forceSpeakerAudioRoute()

    fun forceBluetoothAudioRoute()

    fun isCallLogMissed(): Boolean

    fun addListener(listener: CansListenerStub)

    fun removeListener(listener: CansListenerStub)
}