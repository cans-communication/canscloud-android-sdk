package cc.cans.canscloud.demoappinsdk.notifaication

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavDeepLinkBuilder
import cc.cans.canscloud.demoappinsdk.CansApplication.Companion.coreContext
import cc.cans.canscloud.demoappinsdk.MainActivity
import cc.cans.canscloud.sdk.R
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.demoappinsdk.call.IncomingActivity
import cc.cans.canscloud.demoappinsdk.core.CoreService
import cc.cans.canscloud.sdk.models.AudioState
import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.models.RegisterState
import org.linphone.core.Call
import org.linphone.core.Core
import cc.cans.canscloud.demoappinsdk.compatibility.Compatibility
import cc.cans.canscloud.sdk.Cans.Companion.core
import cc.cans.canscloud.sdk.Cans.Companion.corePreferences

class NotificationsManager(private val context: Context) {

    companion object {
        const val INTENT_REMOTE_ADDRESS = "REMOTE_ADDRESS"
        private const val MISSED_CALL_TAG = "Missed call"
        private const val MISSED_CALLS_NOTIF_ID = 10
        private const val SERVICE_NOTIF_ID = 1
    }

    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }
    var service: CoreService? = null
    private var currentForegroundServiceNotificationId: Int = 0
    private var serviceNotification: Notification? = null

    private val listener = object : CansListenerStub {
        override fun onRegistration(state: RegisterState, message: String?) {
            Log.i("[NotificationsManager]","onRegistration ${state}")
        }

        override fun onUnRegister() {
            Log.i("[NotificationsManager]","onUnRegistration")
        }

        override fun onCallState(core: Core, call: Call, state: CallState, message: String?) {
            Log.i("[NotificationsManager] onCallState: ", "$state")
            when (state) {
                CallState.Idle -> {}
                CallState.IncomingCall -> showIncomingCallNotification(context)
                CallState.StartCall -> {}
                CallState.CallOutgoing -> {}
                CallState.StreamsRunning -> {}
                CallState.Connected -> {}
                CallState.Error -> {}
                CallState.CallEnd -> {}
                CallState.MissCall -> {
                    if (Cans.isCallLogMissed()) {
                        displayMissedCallNotification()
                    }
                }
                CallState.Unknown -> dismissCallNotification()
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

    private fun displayMissedCallNotification() {
        val missedCallCount: Int = Cans.missedCallsCount

        val body: String
        if (missedCallCount > 1) {
            body = context.getString(R.string.missed_call_notification_body).format(missedCallCount)
        } else {
            body = Cans.destinationUsername
        }

        val builder = NotificationCompat.Builder(
            context,
            context.getString(R.string.notification_channel_missed_call_id)
        )
            .setContentTitle(context.getString(R.string.missed_call_notification_title))
            .setContentText(body)
            .setSmallIcon(R.drawable.topbar_missed_call_notification)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setNumber(missedCallCount)
            .setColor(ContextCompat.getColor(context, R.color.notification_led_color))

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

    fun dismissCallNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1)
    }

    fun dismissMissedCallNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(MISSED_CALL_TAG, MISSED_CALLS_NOTIF_ID)
    }

   /* fun startForeground() {
        val serviceChannel = context.getString(R.string.notification_channel_service_id)
        if (Compatibility.getChannelImportance(notificationManager, serviceChannel) == NotificationManagerCompat.IMPORTANCE_NONE) {
            org.linphone.core.tools.Log.w("[Notifications Manager] Service channel is disabled!")
            return
        }

        val coreService = service
        if (coreService != null) {
            startForeground(coreService)
        } else {
            org.linphone.core.tools.Log.w(
                "[Notifications Manager] Can't start service as foreground without a service, starting it now",
            )
            val intent = Intent()
            intent.setClass(context, CoreService::class.java)
            try {
                Compatibility.startForegroundService(coreContext.context, intent)
            } catch (ise: IllegalStateException) {
                org.linphone.core.tools.Log.e("[Notifications Manager] Failed to start Service: $ise")
            } catch (se: SecurityException) {
                org.linphone.core.tools.Log.e("[Notifications Manager] Failed to start Service: $se")
            }
        }
    }

    fun startCallForeground(coreService: CoreService) {
        service = coreService
        when {
            currentForegroundServiceNotificationId != 0 -> {
                if (currentForegroundServiceNotificationId != SERVICE_NOTIF_ID) {
                    org.linphone.core.tools.Log.e(
                        "[Notifications Manager] There is already a foreground service notification [$currentForegroundServiceNotificationId]",
                    )
                } else {
                    org.linphone.core.tools.Log.i(
                        "[Notifications Manager] There is already a foreground service notification, no need to use the call notification to keep Service alive",
                    )
                }
            }
            core.callsNb > 0 -> {
                // When this method will be called, we won't have any notification yet
                val call = core.currentCall ?: core.calls[0]
                when (call.state) {
                    Call.State.IncomingReceived, Call.State.IncomingEarlyMedia -> {
                        org.linphone.core.tools.Log.i(
                            "[Notifications Manager] Creating incoming call notification to be used as foreground service",
                        )
                       // displayIncomingCallNotification(call, true)
                    }
                    else -> {
                        org.linphone.core.tools.Log.i(
                            "[Notifications Manager] Creating call notification to be used as foreground service",
                        )

                      //  displayCallNotification(call, true)
                    }
                }
            }
        }
    }

    private fun startForeground(coreService: CoreService) {
        service = coreService

        val notification = serviceNotification ?: createServiceNotification(false)
        if (notification == null) {
            org.linphone.core.tools.Log.e(
                "[Notifications Manager] Failed to create service notification, aborting foreground service!",
            )
            return
        }

        currentForegroundServiceNotificationId = SERVICE_NOTIF_ID
        org.linphone.core.tools.Log.i(
            "[Notifications Manager] Starting service as foreground [$currentForegroundServiceNotificationId]",
        )

        val core = core
        val isActiveCall = if (core.callsNb > 0) {
            val currentCall = core.currentCall ?: core.calls.first()
            when (currentCall.state) {
                Call.State.IncomingReceived, Call.State.IncomingEarlyMedia, Call.State.OutgoingInit, Call.State.OutgoingProgress, Call.State.OutgoingRinging -> false
                else -> true
            }
        } else {
            false
        }

        Compatibility.startDataSyncForegroundService(
            coreService,
            currentForegroundServiceNotificationId,
            notification,
            isActiveCall,
        )
    }

    fun startForegroundToKeepAppAlive(
        coreService: CoreService,
        useAutoStartDescription: Boolean = true,
    ) {
        service = coreService

        val notification = serviceNotification ?: createServiceNotification(useAutoStartDescription)
        if (notification == null) {
            org.linphone.core.tools.Log.e(
                "[Notifications Manager] Failed to create service notification, aborting foreground service!",
            )
            return
        }

        currentForegroundServiceNotificationId = SERVICE_NOTIF_ID
        org.linphone.core.tools.Log.i(
            "[Notifications Manager] Starting service as foreground [$currentForegroundServiceNotificationId]",
        )

        Compatibility.startDataSyncForegroundService(
            coreService,
            currentForegroundServiceNotificationId,
            notification,
            false,
        )
    }

    private fun startForeground(
        notificationId: Int,
        callNotification: Notification,
        isCallActive: Boolean,
    ) {
        val coreService = service
        if (coreService != null && (currentForegroundServiceNotificationId == 0 || currentForegroundServiceNotificationId == notificationId)) {
            org.linphone.core.tools.Log.i(
                "[Notifications Manager] Starting service as foreground using call notification [$notificationId]",
            )
            try {
                currentForegroundServiceNotificationId = notificationId

                Compatibility.startCallForegroundService(
                    coreService,
                    currentForegroundServiceNotificationId,
                    callNotification,
                    isCallActive,
                )
            } catch (e: Exception) {
                org.linphone.core.tools.Log.e("[Notifications Manager] Foreground service wasn't allowed! $e")
                currentForegroundServiceNotificationId = 0
            }
        } else {
            org.linphone.core.tools.Log.w(
                "[Notifications Manager] Can't start foreground service using notification id [$notificationId] (current foreground service notification id is [$currentForegroundServiceNotificationId]) and service [$coreService]",
            )
        }
    }

    fun stopForegroundNotification() {
        if (service != null) {
            org.linphone.core.tools.Log.i("[Notifications Manager] Stopping service as foreground [$currentForegroundServiceNotificationId]")
            service?.stopForeground(true)
            currentForegroundServiceNotificationId = 0
        }
    }

    fun stopForegroundNotificationIfPossible() {
        if (service != null && currentForegroundServiceNotificationId == SERVICE_NOTIF_ID && !corePreferences.keepServiceAlive) {
            org.linphone.core.tools.Log.i("[Notifications Manager] Stopping auto-started service notification [$currentForegroundServiceNotificationId]")
            stopForegroundNotification()
        }
    }

    fun stopCallForeground() {
        if (service != null && currentForegroundServiceNotificationId != SERVICE_NOTIF_ID && !corePreferences.keepServiceAlive) {
            org.linphone.core.tools.Log.i("[Notifications Manager] Stopping call notification [$currentForegroundServiceNotificationId] used as foreground service")
            stopForegroundNotification()
        }
    }

    private fun createServiceNotification(useAutoStartDescription: Boolean = false): Notification? {
        val serviceChannel = context.getString(R.string.notification_channel_service_id)
        if (Compatibility.getChannelImportance(notificationManager, serviceChannel) == NotificationManagerCompat.IMPORTANCE_NONE) {
            org.linphone.core.tools.Log.w("[Notifications Manager] Service channel is disabled!")
            return null
        }

        val pendingIntent = NavDeepLinkBuilder(context)
            .setComponentName(MainActivity::class.java)
            .createPendingIntent()

        val builder = NotificationCompat.Builder(context, serviceChannel)
            .setContentTitle("Service App")
            .setContentText("Service")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setOngoing(true)
            .setColor(ContextCompat.getColor(context, R.color.primary_color))

        if (!corePreferences.preventInterfaceFromShowingUp) {
            builder.setContentIntent(pendingIntent)
        }

        serviceNotification = builder.build()
        return serviceNotification
    }*/
}