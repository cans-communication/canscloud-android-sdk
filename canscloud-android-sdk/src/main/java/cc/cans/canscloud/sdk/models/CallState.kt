package cc.cans.canscloud.sdk.models

enum class CallState {
    Idle,
    IncomingCall,
    StartCall,
    CallOutgoing,
    Connected,
    StreamsRunning,
    Error,
    CallEnd,
    MissCall,
    Unknown
}
