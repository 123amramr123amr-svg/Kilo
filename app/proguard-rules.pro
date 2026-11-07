# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.personalaiagent.**$$serializer { *; }
-keepclasseswithmembers class com.personalaiagent.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.personalaiagent.**$$serializer { *; }

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
