# canscloud-android-sdk-Tutorial

This tutorial will guide canscloud sdk.
```kotlin```

## Feature
- Register/Unregister
- Incoming Call
- Outgoing Call
- Calling
- Speaker
- Mute
- Listener
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
                RegisterState.OK -> {
                  Log.i("onRegistration: ", "Register Success")
                }
                RegisterState.FAIL -> {
                  Log.i("onRegistration: ", "Register Fail")
                }
            }
        }

        override fun onUnRegister() {}

        override fun onCallState(state: CallState, message: String?) {
            when (state) {
                CallState.CallOutgoing -> {
                  Log.i("[onCallState: ", "Start outgoing call, You can start outgoing activity and display a notification here.")
                }
                CallState.LastCallEnd -> {
                  Log.i("[onCallState: ", "Last call end")
                }
                CallState.IncomingCall -> {
                  Log.i("[onCallState: ", "Start incoming call, You can start the incoming activity and display a notification here.")
                }
                CallState.StartCall -> {
                 Log.i("[onCallState: ", "Start start call")
                }
                CallState.Connected -> {
                  Log.i("[onCallState: ", "Connected Calling , You can start call activity and display a notification here.")
                }
                CallState.Error -> {
                  Log.i("[onCallState: ", "Call Error $message")
                }
                CallState.CallEnd -> {
                  Log.i("[onCallState: ", "Call End")
                }
                CallState.MissCall -> {
                  Log.i("[onCallState: ", "Miss call, You can display a notification miss call here.")
                }
                CallState.Unknown -> {
                  Log.i("[onCallState: ", "Status Other")
                }
            }
        }
    }
```    
```
 Cans.addListener(listener)
```

## Unregister
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

## Mute
```
 Cans.toggleMuteMicrophone()
```

## Speaker
```
 Cans.toggleSpeaker()
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

### License

Copyright © Belledonne Communications




