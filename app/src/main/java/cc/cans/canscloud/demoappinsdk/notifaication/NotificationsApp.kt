package cc.cans.canscloud.demoappinsdk.notifaication

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cc.cans.canscloud.demoappinsdk.R
import cc.cans.canscloud.demoappinsdk.call.CallActivity
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.sdk.Cans.Companion.context
import cc.cans.canscloud.sdk.callback.CallCallback
import cc.cans.canscloud.sdk.core.CoreService
import cc.cans.canscloud.demoappinsdk.call.IncomingActivity

class NotificationsApp(private val context: Context) {

    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    private var service: CoreService? = null
    private var serviceNotification: Notification? = null


    companion object {

        const val INTENT_NOTIF_ID = "NOTIFICATION_ID"
        const val INTENT_REPLY_NOTIF_ACTION = "cc.cans.canscloud.REPLY_ACTION"
        const val INTENT_HANGUP_CALL_NOTIF_ACTION = "cc.cans.canscloud.sdk.HANGUP_CALL_ACTION"
        const val INTENT_ANSWER_CALL_NOTIF_ACTION = "cc.cans.canscloud.sdk.ANSWER_CALL_ACTION"
        const val INTENT_LOCAL_IDENTITY = "LOCAL_IDENTITY"
        const val INTENT_REMOTE_ADDRESS = "REMOTE_ADDRESS"

        private const val SERVICE_NOTIF_ID = 1

        private val listener = object : CallCallback {
            override fun onCallEnd() {
            }

            override fun onCall() {
            }

            @RequiresApi(Build.VERSION_CODES.S)
            override fun onCallOutGoing() {
                displayCallNotification()
            }

            @RequiresApi(Build.VERSION_CODES.S)
            override fun onConnected() {
                displayCallNotification()
                Log.i("Cans Center", "onConnectedCall")
            }

            override fun onError(message: String) {
            }

            override fun onInComingCall() {
                showIncomingCallNotification(Cans.context)
                Log.i("Cans Center", "onInComingCall")
            }

            override fun onLastCallEnd() {
                Log.i("Cans Center", "onLastCallEnd")
            }

            override fun onStartCall() {
                Log.i("Cans Center", "onStartCall")
            }
        }


        fun onCoreReady() {
            Cans.registerCallListener(listener)
            context.startService(Intent(context, IncomingCallService::class.java))
        }

        fun showIncomingCallNotification(context: Context) {
            val incomingCallNotificationIntent = Intent(context, IncomingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)

                putExtra(INTENT_REMOTE_ADDRESS, Cans.remoteAddressCall())
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
            val name = Cans.usernameCall()

            // Build the notification
            val builder = NotificationCompat.Builder(context, "incoming_call_channel")
                .setSmallIcon(R.drawable.ic_launcher_foreground)  // Your call icon here
                .setContentTitle("Incoming Call")
                .setContentText("$name is calling...")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setAutoCancel(true)
                .setOngoing(true)  // Keeps the notification active like a call
                .addAction(cc.cans.canscloud.sdk.R.drawable.call, "Answer", answerPendingIntent)
                .addAction(cc.cans.canscloud.sdk.R.drawable.hang_up, "Decline", declinePendingIntent)
                .setFullScreenIntent(pendingIntent, true)  // Display in full screen
            builder.setContentIntent(pendingIntent)
            // Show the notification
            notificationManager.notify(1, builder.build())
        }

        @RequiresApi(Build.VERSION_CODES.S)
        fun displayCallNotification() {
            val intent = Intent(context, CallActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            context.startActivity(intent)
        }
    }
}