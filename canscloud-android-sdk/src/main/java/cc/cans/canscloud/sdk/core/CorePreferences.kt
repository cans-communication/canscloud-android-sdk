package cc.cans.canscloud.sdk.core

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import cc.cans.canscloud.sdk.core.CoreContextSDK.Companion.cansCenter
import cc.cans.canscloud.sdk.okta.models.LoginInfo
import com.google.gson.Gson
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import org.linphone.core.*
import org.linphone.core.tools.Log
import java.security.KeyStoreException

class CorePreferences constructor(private val context: Context) {
    private var _config: Config? = null
    var config: Config
        get() = _config ?: cansCenter().core.config
        set(value) {
            _config = value
        }

    companion object {
        const val OVERLAY_CLICK_SENSITIVITY = 10

        private const val encryptedSharedPreferencesFile = "encrypted.pref"
    }

    val encryptedSharedPreferences: SharedPreferences? by lazy {
        val masterKey: MasterKey = MasterKey.Builder(
            context,
            MasterKey.DEFAULT_MASTER_KEY_ALIAS
        ).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

        try {
            EncryptedSharedPreferences.create(
                context,
                encryptedSharedPreferencesFile,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (kse: KeyStoreException) {
            Log.e("[VFS] Keystore exception: $kse")
            null
        } catch (e: Exception) {
            Log.e("[VFS] Exception: $e")
            null
        }
    }

    var vfsEnabled: Boolean
        get() = encryptedSharedPreferences?.getBoolean("vfs_enabled", false) ?: false
        set(value) {
            val preferences = encryptedSharedPreferences
            if (preferences == null) {
                Log.e("[VFS] Failed to get encrypted SharedPreferences")
                return
            }

            if (!value && preferences.getBoolean("vfs_enabled", false)) {
                Log.w("[VFS] It is not possible to disable VFS once it has been enabled")
                return
            }

            preferences.edit().putBoolean("vfs_enabled", value)?.apply()
            // When VFS is enabled we disable logcat output for linphone logs
            // TODO: decide if we do it
            // logcatLogsOutput = false
        }

    /* MainActivity */
    var isOpenBatterySettingOS: Boolean
        get() = config.getBool("app", "is_open_battery_setting", false)
        set(value) {
            config.setBool("app", "is_open_battery_setting", value)
        }

    var isBatterySetting: Boolean
        get() = config.getBool("app", "is_battery_setting", false)
        set(value) {
            config.setBool("app", "is_battery_setting", value)
        }

    /* App settings */

    var debugLogs: Boolean
        get() = config.getBool("app", "debug", cc.cans.canscloud.sdk.BuildConfig.DEBUG)
        set(value) {
            config.setBool("app", "debug", value)
        }

    var logcatLogsOutput: Boolean
        get() = config.getBool("app", "print_logs_into_logcat", true)
        set(value) {
            config.setBool("app", "print_logs_into_logcat", value)
        }

    var autoStart: Boolean
        get() = config.getBool("app", "auto_start", true)
        set(value) {
            config.setBool("app", "auto_start", value)
        }

    var keepServiceAlive: Boolean
        get() = config.getBool("app", "keep_service_alive", true)
        set(value) {
            config.setBool("app", "keep_service_alive", value)
        }

    var readAndAgreeTermsAndPrivacy: Boolean
        get() = config.getBool("app", "read_and_agree_terms_and_privacy", false)
        set(value) {
            config.setBool("app", "read_and_agree_terms_and_privacy", value)
        }

    /* UI */

    var forcePortrait: Boolean
        get() = config.getBool("app", "force_portrait_orientation", true)
        set(value) {
            config.setBool("app", "force_portrait_orientation", value)
        }

    var replaceSipUriByUsername: Boolean
        get() = config.getBool("app", "replace_sip_uri_by_username", false)
        set(value) {
            config.setBool("app", "replace_sip_uri_by_username", value)
        }

    // Conference related

    @get:WorkerThread
    @set:WorkerThread
    var createEndToEndEncryptedMeetingsAndGroupCalls: Boolean
        get() = config.getBool("app", "create_e2e_encrypted_conferences", false)
        set(value) {
            config.setBool("app", "create_e2e_encrypted_conferences", value)
        }

    var enableAnimations: Boolean
        get() = config.getBool("app", "enable_animations", false)
        set(value) {
            config.setBool("app", "enable_animations", value)
        }

    /** -1 means auto, 0 no, 1 yes */
    var darkMode: Int
        get() {
            if (!darkModeAllowed) return 0
            return config.getInt("app", "dark_mode", -1)
        }
        set(value) {
            config.setInt("app", "dark_mode", value)
        }

    /** Allow to make screenshots of encrypted chat rooms, disables fragment's secure mode */
    var disableSecureMode: Boolean
        get() = config.getBool("app", "disable_fragment_secure_mode", false)
        set(value) {
            config.setBool("app", "disable_fragment_secure_mode", value)
        }

    /* Audio */

    /* Video */

    var videoPreview: Boolean
        get() = config.getBool("app", "video_preview", false)
        set(value) = config.setBool("app", "video_preview", value)

    /* Chat */

    var preventMoreThanOneFilePerMessage: Boolean
        get() = config.getBool("app", "prevent_more_than_one_file_per_message", false)
        set(value) {
            config.setBool("app", "prevent_more_than_one_file_per_message", value)
        }

    var markAsReadUponChatMessageNotificationDismissal: Boolean
        get() = config.getBool("app", "mark_as_read_notif_dismissal", false)
        set(value) {
            config.setBool("app", "mark_as_read_notif_dismissal", value)
        }

    var makePublicMediaFilesDownloaded: Boolean
        // Keep old name for backward compatibility
        get() = config.getBool("app", "make_downloaded_images_public_in_gallery", true)
        set(value) {
            config.setBool("app", "make_downloaded_images_public_in_gallery", value)
        }

    var useInAppFileViewerForNonEncryptedFiles: Boolean
        get() = config.getBool("app", "use_in_app_file_viewer_for_non_encrypted_files", false)
        set(value) {
            config.setBool("app", "use_in_app_file_viewer_for_non_encrypted_files", value)
        }

    var hideChatMessageContentInNotification: Boolean
        get() = config.getBool("app", "hide_chat_message_content_in_notification", false)
        set(value) {
            config.setBool("app", "hide_chat_message_content_in_notification", value)
        }

    var hideEmptyRooms: Boolean
        get() = config.getBool("app", "hide_empty_chat_rooms", true)
        set(value) {
            config.setBool("app", "hide_empty_chat_rooms", value)
        }

    var hideRoomsFromRemovedProxies: Boolean
        get() = config.getBool("app", "hide_chat_rooms_from_removed_proxies", true)
        set(value) {
            config.setBool("app", "hide_chat_rooms_from_removed_proxies", value)
        }

    var deviceName: String
        get() = config.getString("app", "device_name", "")!!
        set(value) = config.setString("app", "device_name", value)

    var chatRoomShortcuts: Boolean
        get() = config.getBool("app", "chat_room_shortcuts", true)
        set(value) {
            config.setBool("app", "chat_room_shortcuts", value)
        }

    /* Voice Recordings */

    var voiceRecordingMaxDuration: Int
        get() = config.getInt("app", "voice_recording_max_duration", 600000) // in ms
        set(value) = config.setInt("app", "voice_recording_max_duration", value)

    var holdToRecordVoiceMessage: Boolean
        get() = config.getBool("app", "voice_recording_hold_and_release_mode", false)
        set(value) = config.setBool("app", "voice_recording_hold_and_release_mode", value)

    var sendVoiceRecordingRightAway: Boolean
        get() = config.getBool("app", "voice_recording_send_right_away", false)
        set(value) = config.setBool("app", "voice_recording_send_right_away", value)

    /* OnBoardingActivity */

    var isFirstAskedPhoneCall: Boolean
        get() = config.getBool("app", "first_asked_phone_call", true)
        set(value) {
            config.setBool("app", "first_asked_phone_call", value)
        }

    var isFirstAskedNotifications: Boolean
        get() = config.getBool("app", "first_asked_notifications", true)
        set(value) {
            config.setBool("app", "first_asked_notifications", value)
        }

    /* Contacts */

    var storePresenceInNativeContact: Boolean
        get() = config.getBool("app", "store_presence_in_native_contact", false)
        set(value) {
            config.setBool("app", "store_presence_in_native_contact", value)
        }

    var showNewContactAccountDialog: Boolean
        get() = config.getBool("app", "show_new_contact_account_dialog", true)
        set(value) {
            config.setBool("app", "show_new_contact_account_dialog", value)
        }

    var displayOrganization: Boolean
        get() = config.getBool("app", "display_contact_organization", contactOrganizationVisible)
        set(value) {
            config.setBool("app", "display_contact_organization", value)
        }

    var contactsShortcuts: Boolean
        get() = config.getBool("app", "contact_shortcuts", false)
        set(value) {
            config.setBool("app", "contact_shortcuts", value)
        }

    /* Call */

    var callWaiting: Boolean
        get() = config.getBool("app", "call_waiting", false)
        set(value) {
            config.setBool("app", "call_waiting", value)
        }

    var isFirstCallWaiting: Boolean
        get() = config.getBool("app", "show_call_waiting", false)
        set(value) {
            config.setBool("app", "show_call_waiting", value)
        }

    var sendEarlyMedia: Boolean
        get() = config.getBool("sip", "outgoing_calls_early_media", false)
        set(value) {
            config.setBool("sip", "outgoing_calls_early_media", value)
        }

    var acceptEarlyMedia: Boolean
        get() = config.getBool("sip", "incoming_calls_early_media", false)
        set(value) {
            config.setBool("sip", "incoming_calls_early_media", value)
        }

    var autoAnswerEnabled: Boolean
        get() = config.getBool("app", "auto_answer", false)
        set(value) {
            config.setBool("app", "auto_answer", value)
        }

    var autoAnswerDelay: Int
        get() = config.getInt("app", "auto_answer_delay", 0)
        set(value) {
            config.setInt("app", "auto_answer_delay", value)
        }

    // Show overlay inside of application
    var showCallOverlay: Boolean
        get() = config.getBool("app", "call_overlay", true)
        set(value) {
            config.setBool("app", "call_overlay", value)
        }

    // Show overlay even when app is in background, requires permission
    var systemWideCallOverlay: Boolean
        get() = config.getBool("app", "system_wide_call_overlay", false)
        set(value) {
            config.setBool("app", "system_wide_call_overlay", value)
        }

    var callRightAway: Boolean
        get() = config.getBool("app", "call_right_away", false)
        set(value) {
            config.setBool("app", "call_right_away", value)
        }

    var automaticallyStartCallRecording: Boolean
        get() = config.getBool("app", "auto_start_call_record", false)
        set(value) {
            config.setBool("app", "auto_start_call_record", value)
        }

    var useTelecomManager: Boolean
        // Some permissions are required, so keep it to false so user has to manually enable it and give permissions
        get() = config.getBool("app", "use_self_managed_telecom_manager", false)
        set(value) {
            config.setBool("app", "use_self_managed_telecom_manager", value)
            // We need to disable audio focus requests when enabling telecom manager, otherwise it creates conflicts
            config.setBool("audio", "android_disable_audio_focus_requests", value)
        }

    // We will try to auto enable Telecom Manager feature, but in case user disables it don't try again
    var manuallyDisabledTelecomManager: Boolean
        get() = config.getBool("app", "user_disabled_self_managed_telecom_manager", false)
        set(value) {
            config.setBool("app", "user_disabled_self_managed_telecom_manager", value)
        }

    var fullScreenCallUI: Boolean
        get() = config.getBool("app", "full_screen_call", false)
        set(value) {
            config.setBool("app", "full_screen_call", value)
        }

    var routeAudioToBluetoothIfAvailable: Boolean
        get() = config.getBool("app", "route_audio_to_bluetooth_if_available", true)
        set(value) {
            config.setBool("app", "route_audio_to_bluetooth_if_available", value)
        }

    // This won't be done if bluetooth or wired headset is used
    var routeAudioToSpeakerWhenVideoIsEnabled: Boolean
        get() = config.getBool("app", "route_audio_to_speaker_when_video_enabled", true)
        set(value) {
            config.setBool("app", "route_audio_to_speaker_when_video_enabled", value)
        }

    // Automatically handled by SDK
    var pauseCallsWhenAudioFocusIsLost: Boolean
        get() = config.getBool("audio", "android_pause_calls_when_audio_focus_lost", true)
        set(value) {
            config.setBool("audio", "android_pause_calls_when_audio_focus_lost", value)
        }

    /* Assistant */

    var firstStart: Boolean
        get() = config.getBool("app", "first_start", true)
        set(value) {
            config.setBool("app", "first_start", value)
        }

    var xmlRpcServerUrl: String?
        get() = config.getString("assistant", "xmlrpc_url", null)
        set(value) {
            config.setString("assistant", "xmlrpc_url", value)
        }

    /* Dialog related */

    var limeSecurityPopupEnabled: Boolean
        get() = config.getBool("app", "lime_security_popup_enabled", true)
        set(value) {
            config.setBool("app", "lime_security_popup_enabled", value)
        }

    /* Other */

    var voiceMailUri: String?
        get() = config.getString("app", "voice_mail", null)
        set(value) {
            config.setString("app", "voice_mail", value)
        }

    var redirectDeclinedCallToVoiceMail: Boolean
        get() = config.getBool("app", "redirect_declined_call_to_voice_mail", true)
        set(value) {
            config.setBool("app", "redirect_declined_call_to_voice_mail", value)
        }

    var lastUpdateAvailableCheckTimestamp: Int
        get() = config.getInt("app", "version_check_url_last_timestamp", 0)
        set(value) {
            config.setInt("app", "version_check_url_last_timestamp", value)
        }

    var defaultAccountAvatarPath: String?
        get() = config.getString("app", "default_avatar_path", null)
        set(value) {
            config.setString("app", "default_avatar_path", value)
        }

    /* *** Read only application settings, some were previously in non_localizable_custom *** */

    /* UI related */

    val hideContactsWithoutPresence: Boolean
        get() = config.getBool("app", "hide_contacts_without_presence", false)

    val contactOrganizationVisible: Boolean
        get() = config.getBool("app", "display_contact_organization", true)

    val showBorderOnContactAvatar: Boolean
        get() = config.getBool("app", "show_border_on_contact_avatar", false)

    val showBorderOnBigContactAvatar: Boolean
        get() = config.getBool("app", "show_border_on_big_contact_avatar", true)

    private val darkModeAllowed: Boolean
        get() = config.getBool("app", "dark_mode_allowed", true)

    /* Feature related */

    val showScreenshotButton: Boolean
        get() = config.getBool("app", "show_take_screenshot_button_in_call", false)

    val dtmfKeypadVibration: Boolean
        get() = config.getBool("app", "dtmf_keypad_vibraton", false)

    val allowMultipleFilesAndTextInSameMessage: Boolean
        get() = config.getBool("app", "allow_multiple_files_and_text_in_same_message", true)

    val fetchContactsFromDefaultDirectory: Boolean
        get() = config.getBool("app", "fetch_contacts_from_default_directory", true)

    val delayBeforeShowingContactsSearchSpinner: Int
        get() = config.getInt("app", "delay_before_showing_contacts_search_spinner", 200)

    // From Android Contact APIs we can also retrieve the internationalized phone number
    // By default we display the same value as the native address book app
    val preferNormalizedPhoneNumbersFromAddressBook: Boolean
        get() = config.getBool("app", "prefer_normalized_phone_numbers_from_address_book", false)

    val hideStaticImageCamera: Boolean
        get() = config.getBool("app", "hide_static_image_camera", true)

    // Will disable chat feature completely
    val disableChat: Boolean
        get() = config.getBool("app", "disable_chat_feature", false)

    // If enabled, this will cause the video to "freeze" on your correspondent screen
    // as you won't send video packets anymore
    val hideCameraPreviewInPipMode: Boolean
        get() = config.getBool("app", "hide_camera_preview_in_pip_mode", false)

    // This will prevent UI from showing up, except for the launcher & the foreground service notification
    val preventInterfaceFromShowingUp: Boolean
        get() = config.getBool("app", "keep_app_invisible", false)

    // By default we will record voice messages using MKV format and Opus audio encoding
    // If disabled, WAV format will be used instead. Warning: files will be heavier!
    val voiceMessagesFormatMkv: Boolean
        get() = config.getBool("app", "record_voice_messages_in_mkv_format", true)

    val useEphemeralPerDeviceMode: Boolean
        get() = config.getBool("app", "ephemeral_chat_messages_settings_per_device", true)

    // If enabled user will see all ringtones bundled in our SDK
    // and will be able to choose which one to use if not using it's device's default
    val showAllRingtones: Boolean
        get() = config.getBool("app", "show_all_available_ringtones", false)

    // add for OKTA
    var loginInfo: LoginInfo
        get() {
            val json = config.getString("app", "login_info", "")
            return json?.let {
                try {
                    Gson().fromJson(it, LoginInfo::class.java)
                } catch (e: Exception) {
                    LoginInfo("", "", "", "")
                }
            } ?: LoginInfo("", "", "", "")
        }
        set(value) {
            val json = Gson().toJson(value)
            config.setString("app", "login_info", json)
        }

    var isSignInOKTANotConnected: Boolean
        get() = config.getBool("app", "is_sign_in_okta_not_connected", false)
        set(value) {
            val json = Gson().toJson(value)
            config.setBool("app", "is_sign_in_okta_not_connected", value)
        }

    var clientID: String
        get() = config.getString("app", "client_in_okta", "") ?: ""
        set(value) {
            config.setString("app", "client_in_okta", value)
        }

    var discoveryURL: String?
        get() = config.getString("app", "discovery_sign_in_okta", "")
        set(value) {
            config.setString("app", "discovery_sign_in_okta", value)
        }
    // end for OKTA

    /* Default values related */

    val echoCancellerCalibration: Int
        get() = config.getInt("sound", "ec_delay", -1)

    val defaultDomain: String
        get() = config.getString("app", "default_domain", "sip.linphone.org")!!

    val defaultRlsUri: String
        get() = config.getString("sip", "rls_uri", "sips:rls@sip.linphone.org")!!

    val debugPopupCode: String
        get() = config.getString("app", "debug_popup_magic", "#1234#")!!

    val defaultLimeServerUrl: String
        get() = config.getString(
            "lime",
            "lime_server_url",
            "https://lime.linphone.org/lime-server/lime-server.php"
        )!!

    val conferenceServerUri: String
        get() = config.getString(
            "app",
            "default_conference_factory_uri",
            "sip:conference-factory@sip.linphone.org",
        )!!

    val limeX3dhServerUrl: String
        get() = config.getString(
            "app",
            "default_lime_x3dh_server_url",
            "https://lime.linphone.org/lime-server/lime-server.php",
        )!!

    val checkIfUpdateAvailableUrl: String
        get() = config.getString(
            "misc",
            "version_check_url_root",
            "https://linphone.org/releases/android/RELEASE",
        ) ?: ""

    val checkUpdateAvailableInterval: Int
        get() = config.getInt("app", "version_check_interval", 86400000)

    /* Assistant */

    val showCreateAccount: Boolean
        get() = config.getBool("app", "assistant_create_account", true)

    val showLinphoneLogin: Boolean
        get() = config.getBool("app", "assistant_linphone_login", true)

    val showGenericLogin: Boolean
        get() = config.getBool("app", "assistant_generic_login", true)

    val showRemoteProvisioning: Boolean
        get() = config.getBool("app", "assistant_remote_provisioning", true)

    var isRegister: Boolean
        get() = config.getBool("app", "isRegister", false)
        set(value) {
            config.setBool("app", "isRegister", value)
        }

    /* Side Menu */

    val showAccountsInSideMenu: Boolean
        get() = config.getBool("app", "side_menu_accounts", true)

    val showAssistantInSideMenu: Boolean
        get() = config.getBool("app", "side_menu_assistant", true)

    val showSettingsInSideMenu: Boolean
        get() = config.getBool("app", "side_menu_settings", true)

    val showRecordingsInSideMenu: Boolean
        get() = config.getBool("app", "side_menu_recordings", true)

    val showAboutInSideMenu: Boolean
        get() = config.getBool("app", "side_menu_about", true)

    val showQuitInSideMenu: Boolean
        get() = config.getBool("app", "side_menu_quit", true)

    /* Settings */

    val allowDtlsTransport: Boolean
        get() = config.getBool("app", "allow_dtls_transport", false)

    val showAccountSettings: Boolean
        get() = config.getBool("app", "settings_accounts", true)

    val showTunnelSettings: Boolean
        get() = config.getBool("app", "settings_tunnel", true)

    val showAudioSettings: Boolean
        get() = config.getBool("app", "settings_audio", true)

    val showVideoSettings: Boolean
        get() = config.getBool("app", "settings_video", true)

    val showCallSettings: Boolean
        get() = config.getBool("app", "settings_call", true)

    val showChatSettings: Boolean
        get() = config.getBool("app", "settings_chat", true)

    val showNetworkSettings: Boolean
        get() = config.getBool("app", "settings_network", true)

    val showContactsSettings: Boolean
        get() = config.getBool("app", "settings_contacts", true)

    val showAdvancedSettings: Boolean
        get() = config.getBool("app", "settings_advanced", true)

    /* Assets stuff */

    val configPath: String
        get() = context.filesDir.absolutePath + "/.linphonerc"

    val factoryConfigPath: String
        get() = context.filesDir.absolutePath + "/linphonerc"

    val linphoneDefaultValuesPath: String
        get() = context.filesDir.absolutePath + "/assistant_linphone_default_values"

    val defaultValuesPath: String
        get() = context.filesDir.absolutePath + "/assistant_default_values"

    val ringtonesPath: String
        get() = context.filesDir.absolutePath + "/share/sounds/linphone/rings/"

    val defaultRingtonePath: String
        get() = ringtonesPath + "notes_of_the_optimistic.mkv"

    val userCertificatesPath: String
        get() = context.filesDir.absolutePath + "/user-certs"

    val staticPicturePath: String
        get() = context.filesDir.absolutePath + "/share/images/nowebcamcif.jpg"

    fun copyAssetsFromPackage() {
        copy("linphonerc_default", configPath)
        copy("linphonerc_factory", factoryConfigPath, true)
        copy("assistant_linphone_default_values", linphoneDefaultValuesPath, true)
        copy("assistant_default_values", defaultValuesPath, true)

        move(
            context.filesDir.absolutePath + "/linphone-log-history.db",
            context.filesDir.absolutePath + "/call-history.db"
        )
        move(
            context.filesDir.absolutePath + "/zrtp_secrets",
            context.filesDir.absolutePath + "/zrtp-secrets.db"
        )
    }

    fun getString(resource: Int): String {
        return context.getString(resource)
    }

    private fun copy(from: String, to: String, overrideIfExists: Boolean = false) {
        val outFile = File(to)
        if (outFile.exists() && !overrideIfExists) {
            Log.i("copy",
                "[Preferences] File $to already exists"
            )
            return
        }

        val outStream = FileOutputStream(outFile)
        val inFile = context.assets.open(from)
        val buffer = ByteArray(1024)
        var length: Int = inFile.read(buffer)

        while (length > 0) {
            outStream.write(buffer, 0, length)
            length = inFile.read(buffer)
        }

        inFile.close()
        outStream.flush()
        outStream.close()
    }

    private fun move(from: String, to: String, overrideIfExists: Boolean = false) {
        val inFile = File(from)
        val outFile = File(to)
        if (inFile.exists()) {
            if (outFile.exists() && !overrideIfExists) {
                Log.w(
                    "move",
                    "[Preferences] Can't move [$from] to [$to], destination file already exists"
                )
            } else {
                val inStream = FileInputStream(inFile)
                val outStream = FileOutputStream(outFile)

                val buffer = ByteArray(1024)
                var read: Int
                while (inStream.read(buffer).also { read = it } != -1) {
                    outStream.write(buffer, 0, read)
                }

                inStream.close()
                outStream.flush()
                outStream.close()

                inFile.delete()
                Log.i(
                   "move",
                    "[Preferences] Successfully moved [$from] to [$to]"
                )
            }
        } else {
            Log.w(
                "move",
                "[Preferences] Can't move [$from] to [$to], source file doesn't exists"
            )
        }
    }
}
