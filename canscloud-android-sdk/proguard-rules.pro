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
-keepnames class cc.cans.canscloud.sdk.CansCenter { *; }

-keepnames class cc.cans.canscloud.sdk.core.CorePreferences { *; }
-keepnames class cc.cans.canscloud.sdk.core.CoreContextSDK { *; }

# Keep all fields and methods in the companion object
#-keepclassmembers class cc.cans.canscloud.sdk.core.CoreContextSDK$Companion {
#    <fields>;
#    <methods>;
#}

-keep class cc.cans.canscloud.sdk.models.*{ *;}
-keepnames class cc.cans.canscloud.sdk.telecom.TelecomHelper { *; }
-keepnames class cc.cans.canscloud.sdk.telecom.TelecomConnectionService { *; }
-keepnames class cc.cans.canscloud.sdk.telecom.NativeCallWrapper { *; }
-keepnames class cc.cans.canscloud.sdk.utils.AudioRouteUtils { *; }
-keepnames class cc.cans.canscloud.sdk.utils.CansUtils { *; }
-keepnames class cc.cans.canscloud.sdk.utils.PermissionHelper { *; }
-keepnames class cc.cans.canscloud.sdk.utils.SingletonHolder { *; }
-keepnames class cc.cans.canscloud.sdk.compatibility.*{*;}
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





