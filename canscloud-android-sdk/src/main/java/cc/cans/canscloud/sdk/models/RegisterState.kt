package cc.cans.canscloud.sdk.models

enum class RegisterState {
    OK,
    FAIL,
    NONE,
    CLEARED,

    /**
     * Transient: a REGISTER is in flight (liblinphone Progress or Refreshing).
     *
     * An unreachable-but-resolvable proxy takes ~32s to fail over UDP (SIP Timer
     * F); without this the badge silently held its last value that whole time.
     * Matches the "PROGRESS" string iOS publishes from handleRegistrationUpdate.
     */
    PROGRESS
}
