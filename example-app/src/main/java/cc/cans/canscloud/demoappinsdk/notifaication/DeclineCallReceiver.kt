package cc.cans.canscloud.demoappinsdk.notifaication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter

class DeclineCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        cansCenter().terminateCall()
    }
}
