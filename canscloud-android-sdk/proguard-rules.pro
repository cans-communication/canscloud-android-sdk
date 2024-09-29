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

# Keep Cans class and its members
-keep class cc.cans.canscloud.sdk.Cans {
    public *;
}

# Keep Cans companion object
-keep class cc.cans.canscloud.sdk.Cans$Companion {
    public *;
}

# Keep public methods in Cans and its companion object
-keepclassmembers class cc.cans.canscloud.sdk.Cans {
    public *;
}

-keepclassmembers class cc.cans.canscloud.sdk.Cans$Companion {
    public *;
}

# Keep parameter names
-keepattributes Signature
-keepattributes *Annotation






