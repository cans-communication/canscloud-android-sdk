package cc.cans.canscloud.sdk.callback

import cc.cans.canscloud.sdk.models.RegisterState

interface CansRegisterListenerStub {
   fun onRegistration(state: RegisterState, message: String? = null)
   fun onUpdateAccountRegistration(state: RegisterState, message: String? = null)
}