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

# Keep class names but obfuscate method bodies


# Keep all classes and public methods in the closed-source SDK
-keep class cc.cans.canscloud.sdk.** {
    <init>();      # Keep constructors
    public *;      # Keep public methods
}


# Keep all annotations
-keepattributes *Annotation*

# Optionally, keep Kotlin metadata (useful for reflection)
-keep class kotlin.Metadata { *; }

-dontshrink
-printseeds
-verbose
-useuniqueclassmembernames
-printmapping mapping.txt