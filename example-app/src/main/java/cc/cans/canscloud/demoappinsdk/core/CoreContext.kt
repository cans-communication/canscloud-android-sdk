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
import cc.cans.canscloud.demoappinsdk.utils.AudioRouteUtils
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.sdk.Cans.Companion.corePreferences
import cc.cans.canscloud.sdk.Cans.Companion.countCalls
import cc.cans.canscloud.sdk.Cans.Companion.isHeadsetAudioRouteAvailable
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.sdk.models.AudioState
import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.models.RegisterState
import org.linphone.core.Call
import org.linphone.core.Config
import org.linphone.core.Core
import org.linphone.core.Factory

class CoreContext(
    val context: Context,
    coreConfig: Config
    ) : LifecycleOwner, ViewModelStoreOwner {
    private val _lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = _lifecycleRegistry

    private val _viewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore
        get() = _viewModelStore

    val core: Core
    val notificationsManager: NotificationsManager by lazy {
        NotificationsManager(context)
    }
    private var previousCallState = CallState.Idle

    private val listener = object : CansListenerStub {
        override fun onRegistration(state: RegisterState, message: String?) {
            Log.i("[SharedMainViewModel]","onRegistration ${state}")
        }

        override fun onUnRegister() {
            Log.i("[Context]","onUnRegistration")
        }

        override fun onCallState(call: Call,state: CallState, message: String?) {
            Log.i("[Context] onCallState: ","$state")
            when (state) {
                CallState.Idle -> {}
                CallState.IncomingCall -> onIncomingReceived()
                CallState.StartCall -> {}
                CallState.CallOutgoing -> {
                    onOutgoingStarted()
                    if (countCalls == 1 && corePreferences.routeAudioToBluetoothIfAvailable) {
                        AudioRouteUtils.routeAudioToBluetooth(call)
                    }
                }
                CallState.Connected -> onCallStarted()
                CallState.StreamsRunning -> {
                    if (countCalls == 1 && previousCallState == CallState.Connected) {
                        if (isHeadsetAudioRouteAvailable()) {
                            AudioRouteUtils.routeAudioToHeadset(call)
                        } else if (AudioRouteUtils.isBluetoothAudioRouteAvailable()) {
                            AudioRouteUtils.routeAudioToBluetooth(call)
                        }
                    }
                }
                CallState.Error -> {}
                CallState.CallEnd -> {}
                CallState.MissCall -> {}
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
    }

    init {
        Cans.addListener(listener)
        core = Factory.instance().createCoreWithConfig(coreConfig, context)
        notificationsManager.onCoreReady()

        _lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        Log.i("[Context]","Ready")
    }

    /* Start call related activities */

    private fun onIncomingReceived() {
        if (corePreferences.preventInterfaceFromShowingUp) {
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
        if (corePreferences.preventInterfaceFromShowingUp) {
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
        if (corePreferences.preventInterfaceFromShowingUp) {
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
