package cc.cans.canscloud.demoappinsdk.notifaication

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.startForegroundService
import cc.cans.canscloud.demoappinsdk.R
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.sdk.Cans.Companion.context
import cc.cans.canscloud.sdk.CansCloudApplication.Companion.corePreferences
import cc.cans.canscloud.sdk.call.CansCallActivity
import cc.cans.canscloud.sdk.callback.CallCallback
import cc.cans.canscloud.sdk.core.CoreService

class NotificationsApp(private val context: Context) {

    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    private var service: CoreService? = null


    companion object {

        const val INTENT_NOTIF_ID = "NOTIFICATION_ID"
        const val INTENT_REPLY_NOTIF_ACTION = "cc.cans.canscloud.REPLY_ACTION"
        const val INTENT_HANGUP_CALL_NOTIF_ACTION = "cc.cans.canscloud.sdk.HANGUP_CALL_ACTION"
        const val INTENT_ANSWER_CALL_NOTIF_ACTION = "cc.cans.canscloud.sdk.ANSWER_CALL_ACTION"
        const val INTENT_LOCAL_IDENTITY = "LOCAL_IDENTITY"
        const val INTENT_REMOTE_ADDRESS = "REMOTE_ADDRESS"

        private val listener = object : CallCallback {
            override fun onCallEnd() {
            }

            override fun onCallOutGoing() {
            }

            override fun onConnected() {
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
            val incomingCallNotificationIntent = Intent(context, CansCallActivity::class.java).apply {
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

            // Show the notification
            notificationManager.notify(1, builder.build())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startForeground() {
//        val serviceChannel = context.getString(org.linphone.core.R.string.notification_channel_service_id)
//        if (getChannelImportance(notificationManager, serviceChannel) == NotificationManagerCompat.IMPORTANCE_NONE) {
//            Log.w("[Notifications Manager]" ,"Service channel is disabled!")
//            return
//        }
//
//        val coreService = service
//        if (coreService != null) {
//            startForeground(coreService)
//        } else {
//            Log.w(
//                "[Notifications Manager]","Can't start service as foreground without a service, starting it now"
//            )
//            val intent = Intent()
//            intent.setClass(context, CoreService::class.java)
//            try {
//                startForegroundService(context, intent)
//            } catch (ise: IllegalStateException) {
//                Log.e("[Notifications Manager]","Failed to start Service: $ise")
//            } catch (se: SecurityException) {
//                Log.e("[Notifications Manager]", "Failed to start Service: $se")
//            }
//        }
    }

    private fun startForeground(coreService: CoreService) {
        service = coreService

//        val notification = serviceNotification ?: createServiceNotification(false)
//        if (notification == null) {
//            Log.e(
//                "[Notifications Manager] Failed to create service notification, aborting foreground service!"
//            )
//            return
//        }
//
//        currentForegroundServiceNotificationId = SERVICE_NOTIF_ID
//        Log.i(
//            "[Notifications Manager] Starting service as foreground [$currentForegroundServiceNotificationId]"
//        )
//
//        val core = coreContext.core
//        val isActiveCall = if (core.callsNb > 0) {
//            val currentCall = core.currentCall ?: core.calls.first()
//            when (currentCall.state) {
//                Call.State.IncomingReceived, Call.State.IncomingEarlyMedia, Call.State.OutgoingInit, Call.State.OutgoingProgress, Call.State.OutgoingRinging -> false
//                else -> true
//            }
//        } else {
//            false
//        }
//
//        Compatibility.startDataSyncForegroundService(
//            coreService,
//            currentForegroundServiceNotificationId,
//            notification,
//            isActiveCall
//        )
    }

    fun stopForegroundNotificationIfPossible() {
//        if (service != null && currentForegroundServiceNotificationId == SERVICE_NOTIF_ID && !corePreferences.keepServiceAlive) {
//            Log.i("[Notifications Manager] Stopping auto-started service notification [$currentForegroundServiceNotificationId]")
//            stopForegroundNotification()
//        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getChannelImportance(
        notificationManager: NotificationManagerCompat,
        channelId: String,
    ): Int {
//        if (Version.sdkAboveOrEqual(Version.API26_O_80)) {
//            return getChannelImportances(notificationManager, channelId)
//        }
        return NotificationManagerCompat.IMPORTANCE_DEFAULT
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getChannelImportances(
        notificationManager: NotificationManagerCompat,
        channelId: String,
    ): Int {
        val channel = notificationManager.getNotificationChannel(channelId)
        return channel?.importance ?: NotificationManagerCompat.IMPORTANCE_NONE
    }

//    fun getCallAnswerPendingIntent(): PendingIntent {
//        val answerIntent = Intent(Cans.context, AnswerCallReceiver::class.java)
//        answerIntent.action = INTENT_ANSWER_CALL_NOTIF_ACTION
//        answerIntent.putExtra(INTENT_NOTIF_ID, notifiable.notificationId)
//        answerIntent.putExtra(INTENT_REMOTE_ADDRESS, notifiable.remoteAddress)
//
//        return PendingIntent.getBroadcast(
//            context,
//            notificationId,
//            answerIntent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
//        )
//    }

}