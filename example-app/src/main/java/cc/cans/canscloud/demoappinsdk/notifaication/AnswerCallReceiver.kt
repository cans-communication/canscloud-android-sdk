package cc.cans.canscloud.demoappinsdk.notifaication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cans

class AnswerCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        cans.startAnswerCall()
        Toast.makeText(context, "AnswerCallReceiver", Toast.LENGTH_SHORT).show()
    }
}