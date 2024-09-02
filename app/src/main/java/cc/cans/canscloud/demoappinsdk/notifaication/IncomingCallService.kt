package cc.cans.canscloud.demoappinsdk.notifaication

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import cc.cans.canscloud.demoappinsdk.R
import cc.cans.canscloud.sdk.Cans

class IncomingCallService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Show the notification for the incoming call
       // showIncomingCallNotification()

        // Return START_NOT_STICKY because we only want to run the service while handling the call
        return START_NOT_STICKY
    }

    private fun showIncomingCallNotification() {
        // Create an Intent for the Answer action
        val answerIntent = Intent(this, AnswerCallReceiver::class.java)
        val answerPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create an Intent for the Decline action
        val declineIntent = Intent(this, DeclineCallReceiver::class.java)
        val declinePendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val name = Cans.usernameCall()

        // Build the notification
        val notification = NotificationCompat.Builder(this, "incoming_call_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)  // Your call icon here
            .setContentTitle("Incoming Call")
            .setContentText("$name is calling...")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setOngoing(true)  // Keeps the notification active like a call
            .addAction(cc.cans.canscloud.sdk.R.drawable.call, "Answer", answerPendingIntent)
            .addAction(cc.cans.canscloud.sdk.R.drawable.hang_up, "Decline", declinePendingIntent)
            .setFullScreenIntent(null, true)  // Display in full screen
            .build()

        // Start the service in the foreground
        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
