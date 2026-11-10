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

# ===== ZihinPark R8 kuralları =====
# Hata raporlarında satır numaraları
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile
# Enum adları kalıcı veride / rotalarda kullanılıyor
-keepclassmembers enum * { *; }
# Credential Manager (Google ile giriş) – Play Services sağlayıcısı yansıma ile yüklenir
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
# Firestore'a giden veri sınıfları (alan adları korunur)
-keepclassmembers class com.example.cloud.** { <fields>; <init>(...); }
# Room
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn org.slf4j.**
