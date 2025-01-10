package cc.cans.canscloud.demoappinsdk.notifaication

import android.Manifest
import android.app.Notification
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
import cc.cans.canscloud.demoappinsdk.call.CallActivity
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.demoappinsdk.call.IncomingActivity
import cc.cans.canscloud.demoappinsdk.call.OutgoingActivity
import cc.cans.canscloud.sdk.Notifiable
import cc.cans.canscloud.sdk.compatibility.Compatibility
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter
import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.models.RegisterState
import org.linphone.core.Call

class NotificationsManager(private val context: Context) {

    companion object {
        const val INTENT_REMOTE_ADDRESS = "REMOTE_ADDRESS"
        private const val MISSED_CALL_TAG = "Missed call"
        private const val MISSED_CALLS_NOTIF_ID = 2
    }
    private val callNotificationsMap: HashMap<String, Notifiable> = HashMap()

    val notificationManager: NotificationManagerCompat by lazy {
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
                CallState.IncomingCall -> displayIncomingCallNotification(context)
                CallState.Error, CallState.CallEnd -> {
                    dismissCallNotification()
                }
                CallState.MissCall -> {
                    if (cansCenter().isCallLogMissed()) {
                        displayMissedCallNotification()
                    }
                }
                else -> {
                    displayCallNotification(context)
                }
            }
        }

        override fun onLastCallEnded() {
            Log.i("[NotificationsManager]", "onLastCallEnded")
            dismissCallNotification()
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
        cansCenter().addListener(listener)
    }

    fun displayIncomingCallNotification(context: Context) {
        val notifiable = getNotifiableForCall()
        cancel(notifiable.notificationId)

        val incomingCallNotificationIntent = Intent(context, IncomingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)

            putExtra(INTENT_REMOTE_ADDRESS, cansCenter().destinationRemoteAddress)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            incomingCallNotificationIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Answer and Decline intents

        val answerIntent = Intent(context, AnswerCallReceiver::class.java)
        val answerPendingIntent = PendingIntent.getBroadcast(
            context,
            notifiable.notificationId,
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val declineIntent = Intent(context, DeclineCallReceiver::class.java)
        val declinePendingIntent = PendingIntent.getBroadcast(
            context,
            notifiable.notificationId,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val name = cansCenter().destinationUsername

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

        notify(notifiable.notificationId, builder.build())
    }

    private fun displayMissedCallNotification() {
        val missedCallCount: Int = cansCenter().missedCallsCount

        val body: String
        if (missedCallCount > 1) {
            body = context.getString(cc.cans.canscloud.sdk.R.string.missed_call_notification_body).format(missedCallCount)
        } else {
            body = context.getString(cc.cans.canscloud.sdk.R.string.missed_call_notification_body).format(cansCenter().destinationUsername)
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

    fun displayCallNotification(context: Context) {
        val notifiable = getNotifiableForCall()

        val callActivity: Class<*> = when (cansCenter().callCans.state) {
            Call.State.Paused, Call.State.Pausing, Call.State.PausedByRemote -> {
                CallActivity::class.java
            }
            Call.State.OutgoingRinging, Call.State.OutgoingProgress, Call.State.OutgoingInit, Call.State.OutgoingEarlyMedia -> {
                OutgoingActivity::class.java
            }
            else -> {
                CallActivity::class.java
            }
        }

        val serviceChannel = "${context.getString(R.string.app_name)} ${context.getString(cc.cans.canscloud.sdk.R.string.notification_channel_service_id)}"
        val channelToUse = when (Compatibility.getChannelImportance(notificationManager, serviceChannel)) {
            NotificationManagerCompat.IMPORTANCE_NONE -> {
                "${context.getString(R.string.app_name)} ${context.getString(cc.cans.canscloud.sdk.R.string.notification_channel_incoming_call_id)}"
            }
            NotificationManagerCompat.IMPORTANCE_LOW -> {
                // Expected, nothing to do
                serviceChannel
            }
            else -> {
                // If user disables & enabled back service notifications channel, importance won't be low anymore but default!
                serviceChannel
            }
        }

        val callNotificationIntent = Intent(context, callActivity)
        callNotificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifiable.notificationId,
            callNotificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val displayName = ""

        val stringResourceId: Int
        val iconResourceId: Int

        when (cansCenter().callCans.state) {
            Call.State.Paused, Call.State.Pausing, Call.State.PausedByRemote -> {
                stringResourceId = cc.cans.canscloud.sdk.R.string.call_notification_paused
                iconResourceId = cc.cans.canscloud.sdk.R.drawable.topbar_call_paused_notification
            }
            Call.State.OutgoingRinging, Call.State.OutgoingProgress, Call.State.OutgoingInit, Call.State.OutgoingEarlyMedia -> {
                stringResourceId = cc.cans.canscloud.sdk.R.string.call_notification_outgoing
                iconResourceId = cc.cans.canscloud.sdk.R.drawable.topbar_call_notification
            }
            else -> {
                stringResourceId = cc.cans.canscloud.sdk.R.string.call_notification_active
                iconResourceId = cc.cans.canscloud.sdk.R.drawable.topbar_call_notification
            }
        }

        val declineIntent = Intent(context, DeclineCallReceiver::class.java)
        val declinePendingIntent = PendingIntent.getBroadcast(
            context,
            notifiable.notificationId,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(
            context,
            channelToUse,
        )
            .setContentTitle(displayName)
            .setContentText(context.getString(stringResourceId))
            .setSmallIcon(iconResourceId)
           // .setLargeIcon(roundPicture)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setOngoing(true)
            .setColor(ContextCompat.getColor(context, cc.cans.canscloud.sdk.R.color.notification_led_color))
            .addAction(R.drawable.hang_up, "Decline", declinePendingIntent)


        if (!cansCenter().corePreferences.preventInterfaceFromShowingUp) {
            builder.setContentIntent(pendingIntent)
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        notify(notifiable.notificationId, builder.build())
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

    private fun getNotificationIdForCall(): Int {
        return cansCenter().callCans.callLog.startDate.toInt()
    }

    private fun getNotifiableForCall(): Notifiable {
        val address = cansCenter().callCans.remoteAddress.asStringUriOnly()
        var notifiable: Notifiable? = callNotificationsMap[address]
        if (notifiable == null) {
            notifiable = Notifiable(getNotificationIdForCall())
            notifiable.remoteAddress = cansCenter().callCans.remoteAddress.asStringUriOnly()

            callNotificationsMap[address] = notifiable
        }
        return notifiable
    }

    fun dismissCallNotification() {
        val address = cansCenter().destinationRemoteAddress
        val notifiable: Notifiable? = callNotificationsMap[address]
        if (notifiable != null) {
            cancel(notifiable.notificationId)
            callNotificationsMap.remove(address)
        } else {
            Log.w("[Notifications Manager] "," No notification found for call")
        }
    }

    fun dismissMissedCallNotification() {
        cancel(MISSED_CALLS_NOTIF_ID, MISSED_CALL_TAG)
    }

    fun cancel(id: Int, tag: String? = null) {
        Log.i("[Notifications Manager] ","Canceling [$id] with tag [$tag]")
        notificationManager.cancel(tag, id)
    }
}