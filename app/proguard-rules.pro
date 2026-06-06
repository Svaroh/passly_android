# do not obfuscate code - Open Source
-dontobfuscate
-keepattributes SourceFile,LineNumberTable

# R8 full mode (mandatory in AGP 9) strips no-arg constructors that are only
# called via reflection. Many libraries rely on this:
# Firebase ComponentRegistrars, ML Kit factories, etc. Keeping all default
# constructors prevents this entire class of runtime failures.
-keepclassmembers class * {
    <init>();
}

# keep all the code from the app
-keep class net.svaroh.passly.** { *; }

# General config
-keepattributes InnerClasses,Signature,Exceptions,EnclosingMethod,SourceFile,LineNumberTable,*Annotation*
-renamesourcefileattribute SourceFile

# Room & SqlCipher
-keep,includedescriptorclasses class net.sqlcipher.** { *; }
-keep,includedescriptorclasses interface net.sqlcipher.** { *; }

# Gson
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Retrofit
 -keep,allowobfuscation,allowshrinking interface retrofit2.Call
 -keep,allowobfuscation,allowshrinking class retrofit2.Response
 -keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Jwt
-keep class io.fusionauth.jwt.** { *; }
-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.Transient

# Yubikit
-keep class com.yubico.yubikit.android.** { *; }
-dontwarn edu.umd.cs.findbugs.annotations.SuppressFBWarnings
