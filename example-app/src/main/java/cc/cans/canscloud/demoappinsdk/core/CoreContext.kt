package cc.cans.canscloud.demoappinsdk.core

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import cc.cans.canscloud.demoappinsdk.call.CallActivity
import cc.cans.canscloud.demoappinsdk.call.IncomingActivity
import cc.cans.canscloud.demoappinsdk.call.OutgoingActivity
import cc.cans.canscloud.demoappinsdk.notifaication.NotificationsManager
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.sdk.core.CoreContextSDK
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter
import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.models.ConferenceState
import cc.cans.canscloud.sdk.models.RegisterState

class CoreContext(
    val context: Context,
    ) : LifecycleOwner, ViewModelStoreOwner {
    private val _lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = _lifecycleRegistry

    private val _viewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore
        get() = _viewModelStore

    val notificationsManager: NotificationsManager by lazy {
        NotificationsManager(context)
    }
    var stopped = false

    private var previousCallState = CallState.Idle

    private val listener = object : CansListenerStub {
        override fun onRegistration(state: RegisterState, message: String?) {
            Log.i("[SharedMainViewModel]","onRegistration ${state}")
        }

        override fun onUnRegister() {
            Log.i("[Context]","onUnRegistration")
        }

        override fun onCallState(state: CallState, message: String?) {
            Log.i("[Context] onCallState: ","$state")
            when (state) {
                CallState.Idle -> {}
                CallState.IncomingCall -> {
                    onIncomingReceived()
                }
                CallState.StartCall -> {
                    // OutgoingInit
                    onOutgoingStarted()
                }
                CallState.CallOutgoing -> {}
                CallState.Connected -> {
                    onCallStarted()
                }
                CallState.StreamsRunning -> {}
                CallState.Error -> {}
                CallState.CallEnd -> {}
                CallState.MissCall -> {}
                CallState.Pause -> {}
                CallState.Resuming -> {}
                CallState.Unknown -> {}
            }
            previousCallState = state
        }

        override fun onLastCallEnded() {
            Log.i("[Context]", "onLastCallEnded")
        }

        override fun onAudioDeviceChanged() {
            Log.i("[Context onAudioUpdate]", "onAudioDeviceChanged")
        }

        override fun onAudioDevicesListUpdated() {
            Log.i("[Context onAudioUpdate]", "onAudioDevicesListUpdated")
        }

        override fun onConferenceState(state: ConferenceState) {}
    }

    init {
        CoreContextSDK.cansCenter().addListener(listener)
        stopped = false
        _lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        notificationsManager.onCoreReady()
        Log.i("[Context]","Ready")
    }

    /* Start call related activities */

    private fun onIncomingReceived() {
        if (cansCenter().corePreferences.preventInterfaceFromShowingUp) {
            Log.w("[Context]","We were asked to not show the incoming call screen")
            return
        }

        Log.i("[Context]","Starting IncomingCallActivity")
        val intent = Intent(context, IncomingActivity::class.java)
        // This flag is required to start an Activity from a Service context
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun onOutgoingStarted() {
        if (cansCenter().corePreferences.preventInterfaceFromShowingUp) {
            Log.w("[Context]","We were asked to not show the outgoing call screen")
            return
        }

        Log.i("[Context]","Starting OutgoingCallActivity")
        val intent = Intent(context, OutgoingActivity::class.java)
        // This flag is required to start an Activity from a Service context
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun onCallStarted() {
        if (cansCenter().corePreferences.preventInterfaceFromShowingUp) {
            Log.w("[Context]","We were asked to not show the call screen")
            return
        }

        Log.i("[Context]","Starting CallActivity")
        val intent = Intent(context, CallActivity::class.java)
        // This flag is required to start an Activity from a Service context
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        context.startActivity(intent)
    }
}
