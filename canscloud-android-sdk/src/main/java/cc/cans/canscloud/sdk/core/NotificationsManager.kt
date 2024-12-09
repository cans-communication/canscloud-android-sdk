/*
 * Copyright (c) 2010-2020 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cc.cans.canscloud.sdk.core

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.content.LocusIdCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.navigation.NavDeepLinkBuilder
import cc.cans.canscloud.sdk.Cans
import cc.cans.canscloud.sdk.CansCenter
import cc.cans.canscloud.sdk.R
import cc.cans.canscloud.sdk.callback.CansListenerStub
import cc.cans.canscloud.sdk.callback.CoreServiceListener
import cc.cans.canscloud.sdk.compatibility.Compatibility
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter
import cc.cans.canscloud.sdk.models.CallState
import cc.cans.canscloud.sdk.utils.PermissionHelper
import org.linphone.core.*
import org.linphone.core.tools.Log
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

data class Notifiable(val notificationId: Int) {
    val messages: ArrayList<NotifiableMessage> = arrayListOf()

    var isGroup: Boolean = false
    var groupTitle: String? = null
    var localIdentity: String? = null
    var myself: String? = null
    var remoteAddress: String? = null
}

data class NotifiableMessage(
    var message: String,
    val friend: Friend?,
    val sender: String,
    val time: Long,
    val senderAvatar: Bitmap? = null,
    var filePath: Uri? = null,
    var fileMime: String? = null,
    val isOutgoing: Boolean = false,
)

class NotificationsManager(private val context: Context) {
    companion object {
        const val CHAT_NOTIFICATIONS_GROUP = "CHAT_NOTIF_GROUP"
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val INTENT_NOTIF_ID = "NOTIFICATION_ID"
        const val INTENT_REPLY_NOTIF_ACTION = "cc.cans.canscloud.REPLY_ACTION"
        const val INTENT_HANGUP_CALL_NOTIF_ACTION = "org.linphone.HANGUP_CALL_ACTION"
        const val INTENT_ANSWER_CALL_NOTIF_ACTION = "org.linphone.ANSWER_CALL_ACTION"
        const val INTENT_MARK_AS_READ_ACTION = "org.linphone.MARK_AS_READ_ACTION"
        const val INTENT_LOCAL_IDENTITY = "LOCAL_IDENTITY"
        const val INTENT_REMOTE_ADDRESS = "REMOTE_ADDRESS"

        private const val SERVICE_NOTIF_ID = 1
        private const val MISSED_CALLS_NOTIF_ID = 2

        const val CHAT_TAG = "Chat"
        private const val MISSED_CALL_TAG = "Missed call"

    }

    val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }
    private val callNotificationsMap: HashMap<String, Notifiable> = HashMap()

    private var currentForegroundServiceNotificationId: Int = 0
    private var serviceNotification: Notification? = null

    var service: CoreService? = null
    lateinit var callState: CallState

    /* Service related */

    fun startForeground() {
        val serviceChannel = cansCenter().context.getString(R.string.notification_channel_service_id)
        if (Compatibility.getChannelImportance(notificationManager, serviceChannel) == NotificationManagerCompat.IMPORTANCE_NONE) {
            Log.w("[Notifications Manager] Service channel is disabled!")
            return
        }

        val coreService = service
        if (coreService != null) {
            startForeground(coreService)
        } else {
            Log.w(
                "[Notifications Manager] Can't start service as foreground without a service, starting it now",
            )
            val intent = Intent()
            intent.setClass(cansCenter().context, CoreService::class.java)
            try {
                cansCenter().context.startForegroundService(intent)
            } catch (ise: IllegalStateException) {
                Log.e("[Notifications Manager] Failed to start Service: $ise")
            } catch (se: SecurityException) {
                Log.e("[Notifications Manager] Failed to start Service: $se")
            }
        }
    }

    private var listeners = mutableListOf<CansListenerStub>()

    fun startCallForeground(coreService: CoreService) {
        service = coreService
        when {
            currentForegroundServiceNotificationId != 0 -> {
                if (currentForegroundServiceNotificationId != SERVICE_NOTIF_ID) {
                    Log.e(
                        "[Notifications Manager] There is already a foreground service notification [$currentForegroundServiceNotificationId]",
                    )
                } else {
                    Log.i(
                        "[Notifications Manager] There is already a foreground service notification, no need to use the call notification to keep Service alive",
                    )
                }
            }
            cansCenter().core.callsNb > 0 -> {
                // When this method will be called, we won't have any notification yet
                val call = cansCenter().core.currentCall ?: cansCenter().core.calls[0]
                when (call.state) {
                    Call.State.IncomingEarlyMedia, Call.State.IncomingReceived -> {
                        setListenerCall(CallState.IncomingCall)
                    }

                    Call.State.OutgoingInit -> {
                        setListenerCall(CallState.StartCall)
                    }

                    Call.State.OutgoingProgress -> {
                        setListenerCall(CallState.CallOutgoing)
                    }

                    Call.State.StreamsRunning -> {
                        setListenerCall(CallState.StreamsRunning)
                    }

                    Call.State.Connected -> {
                        setListenerCall(CallState.Connected)
                    }

                    Call.State.Error -> {
                        setListenerCall(CallState.Error)
                    }

                    Call.State.End -> {
                        setListenerCall(CallState.CallEnd)
                    }

                    Call.State.Released -> {
                        setListenerCall(CallState.MissCall)
                    }

                    else -> {
                        setListenerCall(CallState.Unknown)
                    }
                }
            }
        }
    }

    private fun setListenerCall(callState: CallState) {
        this.callState = callState
        listeners.forEach { it.onCallState(callState) }
    }

   // private var coreServiceListenerStub = mutableListOf<CoreServiceListener>()

    private var coreServiceListenerStub = object : CoreServiceListener {
        override fun onCreate() {
            TODO("Not yet implemented")
        }

        override fun onStartCommand() {
            TODO("Not yet implemented")
        }

        override fun showForegroundServiceNotification() {
            TODO("Not yet implemented")
        }

        override fun hideForegroundServiceNotification() {
            TODO("Not yet implemented")
        }

        override fun onTaskRemoved() {
            TODO("Not yet implemented")
        }

        override fun onDestroy() {
            TODO("Not yet implemented")
        }

    }

    private fun startForeground(coreService: CoreService) {
        service = coreService

        val notification = serviceNotification ?: createServiceNotification(false)
        if (notification == null) {
            Log.e(
                "[Notifications Manager] Failed to create service notification, aborting foreground service!",
            )
            return
        }

        currentForegroundServiceNotificationId = SERVICE_NOTIF_ID
        Log.i(
            "[Notifications Manager] Starting service as foreground [$currentForegroundServiceNotificationId]",
        )

        val core = cansCenter().core
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
            Log.e(
                "[Notifications Manager] Failed to create service notification, aborting foreground service!",
            )
            return
        }

        currentForegroundServiceNotificationId = SERVICE_NOTIF_ID
        Log.i(
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
            Log.i(
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
                Log.e("[Notifications Manager] Foreground service wasn't allowed! $e")
                currentForegroundServiceNotificationId = 0
            }
        } else {
            Log.w(
                "[Notifications Manager] Can't start foreground service using notification id [$notificationId] (current foreground service notification id is [$currentForegroundServiceNotificationId]) and service [$coreService]",
            )
        }
    }

    fun stopForegroundNotification() {
        if (service != null) {
            Log.i("[Notifications Manager] Stopping service as foreground [$currentForegroundServiceNotificationId]")
            service?.stopForeground(true)
            currentForegroundServiceNotificationId = 0
        }
    }

    fun stopForegroundNotificationIfPossible() {
//        if (service != null && currentForegroundServiceNotificationId == SERVICE_NOTIF_ID && !corePreferences.keepServiceAlive) {
//            Log.i("[Notifications Manager] Stopping auto-started service notification [$currentForegroundServiceNotificationId]")
//            stopForegroundNotification()
//        }
    }

    fun stopCallForeground() {
//        if (service != null && currentForegroundServiceNotificationId != SERVICE_NOTIF_ID && !corePreferences.keepServiceAlive) {
//            Log.i("[Notifications Manager] Stopping call notification [$currentForegroundServiceNotificationId] used as foreground service")
//            stopForegroundNotification()
//        }
    }

    fun serviceCreated(createdService: CoreService) {
        Log.i("[Notifications Manager] Service has been created, keeping it around")
        service = createdService
    }

    fun serviceDestroyed() {
        Log.i("[Notifications Manager] Service has been destroyed")
        stopForegroundNotification()
        service = null
    }

    private fun createServiceNotification(useAutoStartDescription: Boolean = false): Notification? {
        val serviceChannel = context.getString(R.string.notification_channel_service_id)
        if (Compatibility.getChannelImportance(notificationManager, serviceChannel) == NotificationManagerCompat.IMPORTANCE_NONE) {
            Log.w("[Notifications Manager] Service channel is disabled!")
            return null
        }

        val pendingIntent = NavDeepLinkBuilder(context)
//            .setComponentName(MainActivity::class.java)
//            .setGraph(R.navigation.main_nav_graph)
//            .setDestination(R.id.dialerFragment)
            .createPendingIntent()

        val builder = NotificationCompat.Builder(context, serviceChannel)
            .setContentTitle(context.getString(R.string.service_name))
            .setContentText(if (useAutoStartDescription) context.getString(R.string.service_auto_start_description) else context.getString(R.string.service_description))
            .setSmallIcon(R.drawable.call)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setOngoing(true)
            .setColor(ContextCompat.getColor(context, R.color.primary_color))

        //builder.setContentIntent(pendingIntent)

        serviceNotification = builder.build()
        return serviceNotification
    }

    /* Call related */

    private fun getNotificationIdForCall(call: Call): Int {
        return call.callLog.startDate.toInt()
    }

    private fun getNotifiableForCall(call: Call): Notifiable {
        val address = call.remoteAddress.asStringUriOnly()
        var notifiable: Notifiable? = callNotificationsMap[address]
        if (notifiable == null) {
            notifiable = Notifiable(getNotificationIdForCall(call))
            notifiable.remoteAddress = call.remoteAddress.asStringUriOnly()

            callNotificationsMap[address] = notifiable
        }
        return notifiable
    }

//    fun getPerson(friend: Friend?, displayName: String, picture: Bitmap?): Person {
//        return if (friend != null) {
//            friend.getPerson()
//        } else {
//            val builder = Person.Builder().setName(displayName)
//            val userIcon =
//                if (picture != null) {
//                    IconCompat.createWithAdaptiveBitmap(picture)
//                } else {
//                    IconCompat.createWithResource(context, R.drawable.profile)
//                }
//            if (userIcon != null) builder.setIcon(userIcon)
//            builder.build()
//        }
//    }

    /* Notifications actions */

//    fun getCallAnswerPendingIntent(notifiable: Notifiable): PendingIntent {
//        val answerIntent = Intent(context, NotificationBroadcastReceiver::class.java)
//        answerIntent.action = INTENT_ANSWER_CALL_NOTIF_ACTION
//        answerIntent.putExtra(INTENT_NOTIF_ID, notifiable.notificationId)
//        answerIntent.putExtra(INTENT_REMOTE_ADDRESS, notifiable.remoteAddress)
//
//        return PendingIntent.getBroadcast(
//            context,
//            notifiable.notificationId,
//            answerIntent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
//        )
//    }
//
//    fun getCallAnswerAction(notifiable: Notifiable): NotificationCompat.Action {
//        return NotificationCompat.Action.Builder(
//            R.drawable.call_audio_start,
//            context.getString(R.string.incoming_call_notification_answer_action_label),
//            getCallAnswerPendingIntent(notifiable),
//        ).build()
//    }
//
//    fun getCallDeclinePendingIntent(notifiable: Notifiable): PendingIntent {
//        val hangupIntent = Intent(context, NotificationBroadcastReceiver::class.java)
//        hangupIntent.action = INTENT_HANGUP_CALL_NOTIF_ACTION
//        hangupIntent.putExtra(INTENT_NOTIF_ID, notifiable.notificationId)
//        hangupIntent.putExtra(INTENT_REMOTE_ADDRESS, notifiable.remoteAddress)
//
//        return PendingIntent.getBroadcast(
//            context,
//            notifiable.notificationId,
//            hangupIntent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
//        )
//    }
//
//    fun getCallDeclineAction(notifiable: Notifiable): NotificationCompat.Action {
//        return NotificationCompat.Action.Builder(
//            R.drawable.call_hangup,
//            context.getString(R.string.incoming_call_notification_hangup_action_label),
//            getCallDeclinePendingIntent(notifiable),
//        ).build()
//    }
}
