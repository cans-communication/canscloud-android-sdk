package cc.cans.canscloud.sdk.callback

import cc.cans.canscloud.sdk.models.RegisterState

interface CansRegisterAccountListenerStub {
   fun onRegistration(state: RegisterState, message: String? = null)
}