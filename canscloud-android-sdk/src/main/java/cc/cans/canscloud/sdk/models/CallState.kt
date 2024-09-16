package cc.cans.canscloud.sdk.models

enum class CallState {
    IncomingCall,
    StartCall,
    CallOutgoing,
    StreamsRunning,
    Connected,
    Error,
    CallEnd,
    LastCallEnd,
    MissCall,
    Unknown
}
