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

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

#-keepclasseswithmembernames class * {
#    native <methods>;
#}
#-keepnames class ** { *; }

#-keep class cc.cans.canscloud.sdk.Cans$Companion {
#  public void config(android.content.Context, java.lang.String);
#}
#
#-keep class cc.cans.canscloud.sdk.Cans {
#    public static *;
#}

-keepclassmembers class cc.cans.canscloud.sdk.Cans {
    public static ** Companion;
}

-keep class cc.cans.canscloud.sdk.callback.CansListenerStub {
    public *;
}

-keep class cc.cans.canscloud.sdk.compatibility.*{
    public *;
}

-keep class cc.cans.canscloud.sdk.core.CoreContextSDK {
    public *;
}

-keep class cc.cans.canscloud.sdk.core.CorePreferences {
   public *;
}

-keep class cc.cans.canscloud.sdk.models.*{
    public *;
}

-keep class cc.cans.canscloud.sdk.telecom.*{
    public *;
}

-keep class cc.cans.canscloud.sdk.utils.*{
    public *;
}






