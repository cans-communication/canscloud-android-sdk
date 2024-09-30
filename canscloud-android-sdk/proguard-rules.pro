# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# Keep the interface and all its methods/properties
-keep interface cc.cans.canscloud.sdk.Cans {
    *;
}

# Keep the names of classes, methods, and fields for your interface and its implementation
-keep class cc.cans.canscloud.sdk.CansCenter {
    <fields>;
    <methods>;
}

-keep class cc.cans.canscloud.sdk.core.CorePreferences {
    <fields>;
    <methods>;
}
-keep class cc.cans.canscloud.sdk.core.CoreContextSDK {
    <fields>;
    <methods>;
}
-keep class cc.cans.canscloud.sdk.models.*{ *;}
-keep class cc.cans.canscloud.sdk.telecom.TelecomHelper
-keep class cc.cans.canscloud.sdk.telecom.TelecomConnectionService
-keep class cc.cans.canscloud.sdk.telecom.NativeCallWrapper
-keep class cc.cans.canscloud.sdk.utils.AudioRouteUtils
-keep class cc.cans.canscloud.sdk.utils.CansUtils
-keep class cc.cans.canscloud.sdk.utils.PermissionHelper
-keep class cc.cans.canscloud.sdk.utils.SingletonHolder
-keep class cc.cans.canscloud.sdk.compatibility.*{*;}
-keep class cc.cans.canscloud.sdk.callback.CansListenerStub { *; }



#
#-keep class cc.cans.canscloud.sdk.callback.CansListenerStub { *; }
#-keep class cc.cans.canscloud.sdk.compatibility.*{*;}
#
#-keep class cc.cans.canscloud.sdk.core.CoreContextSDK { *;}
#-keep class cc.cans.canscloud.sdk.core.CorePreferences { *;}
#-keep class cc.cans.canscloud.sdk.models.*{ *;}
#-keep class cc.cans.canscloud.sdk.telecom.*{ *;}
#-keep class cc.cans.canscloud.sdk.utils.*{ *;}
#





