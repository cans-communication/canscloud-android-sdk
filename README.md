# canscloud-android-sdk

This tutorial will guide canscloud sdk.
```kotlin```

## Feature
- Register/UnRegister
- Incoming Call
- Outgoing Call
- Calling
- Speaker
- Mute
- Notification

# Usage
Add the dependency to your app's build.gradle:
```
 implementation 'com.github.cans-communication:canscloud-android-sdk:0.1.39'
```

Add the dependency to your app's settings.gradle:
```
    maven { url 'https://jitpack.io' }
    maven {
        name "linphone.org maven repository"
        url "https://linphone.org/maven_repository"
        content {
            includeGroup "org.linphone"
        }
    }
```

## Config
```
 Cans.config(this, packageManager, packageName)
```

## Register
```
 Cans.register("username","password","domian","port",CansTransport)
```
### Listener
```
 private val listener = object : CansListenerStub {
        override fun onRegistration(state: RegisterState, message: String?) {
            when (state) {
                RegisterState.OK -> {}
                RegisterState.FAIL -> {}
            }
        }

        override fun onUnRegister() {}

        override fun onCallState(state: CallState, message: String?) {
            when (state) {
                CallState.CallOutgoing -> {}
                CallState.LastCallEnd -> {}
                CallState.IncomingCall -> {}
                CallState.StartCall -> {}
                CallState.Connected -> {}
                CallState.Error -> {}
                CallState.CallEnd -> {}
                CallState.MissCall -> {}
                CallState.Unknown -> {}
            }
        }
    }
```    
```
 Cans.addListener(listener)
```

## UnRegister
```
 Cans.removeAccount()
```
### Remove Listener
```
 Cans.removeListener(listener)
```

## Start Call
```
 Cans.startCall(binding.editTextPhoneNumber.text.toString())
```

## Hang Up
```
 Cans.terminateCall()
```

## Answer Call
```
 Cans.startAnswerCall()
```

## Miss Call Count
```
 Cans.missedCallsCount
```

## Call Count
```
 Cans.countCalls
```

## Duration Call
```
 Cans.durationTime
```

```
 binding.activeCallTimer.visibility = View.VISIBLE
 binding.activeCallTimer.base = SystemClock.elapsedRealtime() - (1000 * Cans.durationTime)
 binding.activeCallTimer.start()
```

## Mute
```
 Cans.toggleMuteMicrophone()
```

## Speaker
```
 Cans.toggleSpeaker()
```

# Notification

### Incoming call
```
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
        notificationManager.notify(1, builder.build())
        context.startForegroundService(incomingCallNotificationIntent)

    }
```

### Miss Call
```
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
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setNumber(missedCallCount)
            .setColor(ContextCompat.getColor(context, R.color.notification_led_color))

        val notification = builder.build()
        notify(MISSED_CALLS_NOTIF_ID, notification, MISSED_CALL_TAG)
    }
```

# Permission (Request)
## Notification
```
    Manifest.permission.POST_NOTIFICATIONS
```
### Record Audio
```
    Manifest.permission.RECORD_AUDIO
```

