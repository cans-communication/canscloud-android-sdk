package cc.cans.canscloud.demoappinsdk.notifaication

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import cc.cans.canscloud.demoappinsdk.R
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.demoappinsdk.call.IncomingActivity
import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.models.RegisterState

class NotificationsManager(private val context: Context) {

    companion object {
        const val INTENT_REMOTE_ADDRESS = "REMOTE_ADDRESS"
        private const val MISSED_CALL_TAG = "Missed call"
        private const val MISSED_CALLS_NOTIF_ID = 10
    }

    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    private val listener = object : CansListenerStub {
        override fun onRegistration(state: RegisterState, message: String?) {
            Log.i("[NotificationsManager]","onRegistration ${state}")
        }

        override fun onUnRegister() {
            Log.i("[NotificationsManager]","onUnRegistration")
        }

        override fun onCallState(state: CallState, message: String?) {
            Log.i("[NotificationsManager] onCallState: ", "$state")
            when (state) {
                CallState.Idle -> {}
                CallState.IncomingCall -> showIncomingCallNotification(context)
                CallState.StartCall -> {}
                CallState.CallOutgoing -> {}
                CallState.StreamsRunning -> {}
                CallState.Connected -> {
                    dismissIncomingCallNotification()
                }
                CallState.Error -> {}
                CallState.CallEnd -> {}
                CallState.MissCall -> {
                    if (Cans.isCallLogMissed()) {
                        displayMissedCallNotification()
                    }
                }
                CallState.Unknown -> dismissIncomingCallNotification()
            }
        }

        override fun onLastCallEnded() {
            Log.i("[NotificationsManager]", "onLastCallEnded")
            dismissIncomingCallNotification()
        }

        override fun onAudioDeviceChanged() {
            Log.i("[Context onAudioUpdate]", "onAudioDeviceChanged")
        }

        override fun onAudioDevicesListUpdated() {
            Log.i("[Context onAudioUpdate]", "onAudioDevicesListUpdated")
        }
    }

    init {
        onCoreReady()
    }

    fun onCoreReady() {
        Cans.addListener(listener)
    }

    fun showIncomingCallNotification(context: Context) {
        val incomingCallNotificationIntent = Intent(context, IncomingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)

            putExtra(INTENT_REMOTE_ADDRESS, Cans.destinationRemoteAddress)
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
            "${context.getString(R.string.app_name)} ${context.getString(cc.cans.canscloud.sdk.R.string.notification_channel_incoming_call_id)}"
        )
            .setSmallIcon(cc.cans.canscloud.sdk.R.drawable.topbar_call_notification)
            .setContentTitle("${context.getString(R.string.app_name)} ${context.getString(cc.cans.canscloud.sdk.R.string.notification_channel_incoming_call_name)}")
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

    private fun displayMissedCallNotification() {
        val missedCallCount: Int = Cans.missedCallsCount

        val body: String
        if (missedCallCount > 1) {
            body = context.getString(cc.cans.canscloud.sdk.R.string.missed_call_notification_body).format(missedCallCount)
        } else {
            body = context.getString(cc.cans.canscloud.sdk.R.string.missed_call_notification_body).format(Cans.destinationUsername)
        }

        val builder = NotificationCompat.Builder(
            context,
            "${context.getString(R.string.app_name)} ${context.getString(cc.cans.canscloud.sdk.R.string.notification_channel_missed_call_id)}")
            .setContentTitle("${context.getString(R.string.app_name)} ${context.getString(cc.cans.canscloud.sdk.R.string.missed_call_notification_title)}")
            .setContentText(body)
            .setSmallIcon(cc.cans.canscloud.sdk.R.drawable.topbar_missed_call_notification)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setNumber(missedCallCount)
            .setColor(ContextCompat.getColor(context, cc.cans.canscloud.sdk.R.color.notification_led_color))

        val notification = builder.build()
        notify(MISSED_CALLS_NOTIF_ID, notification, MISSED_CALL_TAG)
    }

    private fun notify(id: Int, notification: Notification, tag: String? = null) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationManager.notify(tag, id, notification)
    }

    fun dismissIncomingCallNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1)
    }

    fun dismissMissedCallNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(MISSED_CALL_TAG, MISSED_CALLS_NOTIF_ID)
    }
}