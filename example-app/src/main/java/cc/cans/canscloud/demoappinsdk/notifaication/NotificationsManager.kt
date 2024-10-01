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
import cc.cans.canscloud.demoappinsdk.call.CallActivity
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.demoappinsdk.call.IncomingActivity
import cc.cans.canscloud.demoappinsdk.call.OutgoingActivity
import cc.cans.canscloud.sdk.CansCenter
import cc.cans.canscloud.sdk.Notifiable
import cc.cans.canscloud.sdk.compatibility.Compatibility
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter
import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.models.RegisterState
import org.linphone.core.Call
import org.linphone.core.tools.service.CoreService

class NotificationsManager(private val context: Context) {

    companion object {
        const val INTENT_REMOTE_ADDRESS = "REMOTE_ADDRESS"
        private const val MISSED_CALL_TAG = "Missed call"
        private const val MISSED_CALLS_NOTIF_ID = 2
        private const val SERVICE_NOTIF_ID = 1

    }
    private val callNotificationsMap: HashMap<String, Notifiable> = HashMap()
    private var currentForegroundServiceNotificationId: Int = 0
    var service: CoreService? = null

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
                CallState.IncomingCall -> showIncomingCallNotification(context)
                CallState.Error, CallState.CallEnd -> {
                    dismissCallNotification()
                }
                CallState.MissCall -> {
                    if (cansCenter().isCallLogMissed()) {
                        displayMissedCallNotification()
                    }
                }
                else -> {
                    dismissCallNotification()
                    displayCallNotification(true)
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

    fun showIncomingCallNotification(context: Context) {
        val notifiable = getNotifiableForCall()
        if (notifiable.notificationId == currentForegroundServiceNotificationId) {
            cancel(notifiable.notificationId)
            currentForegroundServiceNotificationId = 0
        }

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

    fun displayCallNotification(isCallActive: Boolean) {
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

        val serviceChannel = context.getString(cc.cans.canscloud.sdk.R.string.notification_channel_service_id)
        val channelToUse = when (Compatibility.getChannelImportance(notificationManager, serviceChannel)) {
            NotificationManagerCompat.IMPORTANCE_NONE -> {
                context.getString(cc.cans.canscloud.sdk.R.string.notification_channel_incoming_call_id)
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

        val displayName = CansCenter().username

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

//        val coreService = service
//        if (coreService != null && (currentForegroundServiceNotificationId == 0 || currentForegroundServiceNotificationId == notifiable.notificationId)) {
//            org.linphone.core.tools.Log.i(
//                "[Notifications Manager] Notifying call notification for foreground service [${notifiable.notificationId}]",
//            )
//            startForeground(notifiable.notificationId, notification, isCallActive)
//        } else if (coreService != null && currentForegroundServiceNotificationId == SERVICE_NOTIF_ID) {
//            // To add microphone & camera foreground service use to foreground service if needed
//            startForeground(coreService)
//        }
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
        cancel(MISSED_CALLS_NOTIF_ID, MISSED_CALL_TAG)
    }

    fun dismissMissedCallNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(MISSED_CALL_TAG, MISSED_CALLS_NOTIF_ID)
    }

    fun cancel(id: Int, tag: String? = null) {
        Log.i("[Notifications Manager] ","Canceling [$id] with tag [$tag]")
        notificationManager.cancel(tag, id)
    }


//    fun startCallForeground(coreService: CoreService) {
//        service = coreService
//        when {
//            currentForegroundServiceNotificationId != 0 -> {
//                if (currentForegroundServiceNotificationId != SERVICE_NOTIF_ID) {
//                    org.linphone.core.tools.Log.e(
//                        "[Notifications Manager] There is already a foreground service notification [$currentForegroundServiceNotificationId]",
//                    )
//                } else {
//                    org.linphone.core.tools.Log.i(
//                        "[Notifications Manager] There is already a foreground service notification, no need to use the call notification to keep Service alive",
//                    )
//                }
//            }
//            cansCenter().countCalls > 0 -> {
//                // When this method will be called, we won't have any notification yet
//                val call = cansCenter().coreContext.core.currentCall ?: coreContext.core.calls[0]
//                when (call.state) {
//                    Call.State.IncomingReceived, Call.State.IncomingEarlyMedia -> {
//                        org.linphone.core.tools.Log.i(
//                            "[Notifications Manager] Creating incoming call notification to be used as foreground service",
//                        )
//                        displayIncomingCallNotification(call, true)
//                    }
//                    else -> {
//                        org.linphone.core.tools.Log.i(
//                            "[Notifications Manager] Creating call notification to be used as foreground service",
//                        )
//                        displayCallNotification(call, true)
//                    }
//                }
//            }
//        }
//    }
//
//    private fun startForeground(coreService: CoreService) {
//        service = coreService
//
//        val notification = serviceNotification ?: createServiceNotification(false)
//        if (notification == null) {
//            org.linphone.core.tools.Log.e(
//                "[Notifications Manager] Failed to create service notification, aborting foreground service!",
//            )
//            return
//        }
//
//        currentForegroundServiceNotificationId = SERVICE_NOTIF_ID
//        org.linphone.core.tools.Log.i(
//            "[Notifications Manager] Starting service as foreground [$currentForegroundServiceNotificationId]",
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
//            isActiveCall,
//        )
//    }
//
//    fun startForegroundToKeepAppAlive(
//        coreService: CoreService,
//        useAutoStartDescription: Boolean = true,
//    ) {
//        service = coreService
//
//        val notification = serviceNotification ?: createServiceNotification(useAutoStartDescription)
//        if (notification == null) {
//            org.linphone.core.tools.Log.e(
//                "[Notifications Manager] Failed to create service notification, aborting foreground service!",
//            )
//            return
//        }
//
//        currentForegroundServiceNotificationId = SERVICE_NOTIF_ID
//        org.linphone.core.tools.Log.i(
//            "[Notifications Manager] Starting service as foreground [$currentForegroundServiceNotificationId]",
//        )
//
//        Compatibility.startDataSyncForegroundService(
//            coreService,
//            currentForegroundServiceNotificationId,
//            notification,
//            false,
//        )
//    }
//
//    private fun startForeground(
//        notificationId: Int,
//        callNotification: Notification,
//        isCallActive: Boolean,
//    ) {
//        val coreService = service
//        if (coreService != null && (currentForegroundServiceNotificationId == 0 || currentForegroundServiceNotificationId == notificationId)) {
//            org.linphone.core.tools.Log.i(
//                "[Notifications Manager] Starting service as foreground using call notification [$notificationId]",
//            )
//            try {
//                currentForegroundServiceNotificationId = notificationId
//
//                Compatibility.startCallForegroundService(
//                    coreService,
//                    currentForegroundServiceNotificationId,
//                    callNotification,
//                    isCallActive,
//                )
//            } catch (e: Exception) {
//                org.linphone.core.tools.Log.e("[Notifications Manager] Foreground service wasn't allowed! $e")
//                currentForegroundServiceNotificationId = 0
//            }
//        } else {
//            org.linphone.core.tools.Log.w(
//                "[Notifications Manager] Can't start foreground service using notification id [$notificationId] (current foreground service notification id is [$currentForegroundServiceNotificationId]) and service [$coreService]",
//            )
//        }
//    }
//
//    fun stopForegroundNotification() {
//        if (service != null) {
//            org.linphone.core.tools.Log.i("[Notifications Manager] Stopping service as foreground [$currentForegroundServiceNotificationId]")
//            service?.stopForeground(true)
//            currentForegroundServiceNotificationId = 0
//        }
//    }
//
//    fun stopForegroundNotificationIfPossible() {
//        if (service != null && currentForegroundServiceNotificationId == SERVICE_NOTIF_ID && !corePreferences.keepServiceAlive) {
//            org.linphone.core.tools.Log.i("[Notifications Manager] Stopping auto-started service notification [$currentForegroundServiceNotificationId]")
//            stopForegroundNotification()
//        }
//    }
//
//    fun stopCallForeground() {
//        if (service != null && currentForegroundServiceNotificationId != SERVICE_NOTIF_ID && !corePreferences.keepServiceAlive) {
//            org.linphone.core.tools.Log.i("[Notifications Manager] Stopping call notification [$currentForegroundServiceNotificationId] used as foreground service")
//            stopForegroundNotification()
//        }
//    }
}