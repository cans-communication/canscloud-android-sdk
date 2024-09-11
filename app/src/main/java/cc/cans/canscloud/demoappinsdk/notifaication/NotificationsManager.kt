package cc.cans.canscloud.demoappinsdk.notifaication

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import cc.cans.canscloud.sdk.R
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.demoappinsdk.call.IncomingActivity
import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.models.RegisterState

class NotificationsManager(private val context: Context) {

    companion object {
        const val INTENT_REMOTE_ADDRESS = "REMOTE_ADDRESS"
    }

    private val listener = object : CansListenerStub {
        override fun onRegistration(state: RegisterState, message: String) {
            Log.i("[SharedMainViewModel]","onRegistration ${state}")
        }

        override fun onUnRegister() {
            Log.i("[Context]","onUnRegistration")
        }

        @RequiresApi(Build.VERSION_CODES.S)
        override fun onCallState(state: CallState, message: String) {
            Log.i("[NotificationsApp] onCallState: ", "$state")
            when (state) {
                CallState.CallOutgoing -> {}
                CallState.LastCallEnd -> {}
                CallState.IncomingCall -> showIncomingCallNotification(context)
                CallState.StartCall -> {}
                CallState.Connected -> {}
                CallState.Error -> {}
                CallState.CallEnd -> {}
                CallState.Unknown -> {}
            }
        }
    }

    init {
        onCoreReady()
    }

    fun onCoreReady() {
        Cans.coreListeners.add(listener)
    }

    fun showIncomingCallNotification(context: Context) {
        val incomingCallNotificationIntent = Intent(context, IncomingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)

            putExtra(INTENT_REMOTE_ADDRESS, Cans.remoteAddressCall)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            incomingCallNotificationIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Answer and Decline intents

        val answerIntent = Intent(context, AnswerCallReceiver::class.java)
        val answerPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val declineIntent = Intent(context, DeclineCallReceiver::class.java)
        val declinePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val name = Cans.destinationUsername

        // Build the notification
        val builder = NotificationCompat.Builder(
            context,
            context.getString(R.string.notification_channel_incoming_call_id)
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_channel_incoming_call_name))
            .setContentText("$name is calling...")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setOngoing(true)
            .addAction(R.drawable.call, "Answer", answerPendingIntent)
            .addAction(R.drawable.hang_up, "Decline", declinePendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
        // Show the notification
        notificationManager.notify(1, builder.build())
    }
}