# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep class * extends dagger.hilt.android.HiltActivity

# Kotlinx Serialization
-keep class kotlinx.serialization.** { *; }
-keep @kotlinx.serialization.Serializable class *

# Retrofit/Moshi
-keep class com.squareup.moshi.** { *; }
-keep class retrofit2.** { *; }

# OkHttp
-keep class okhttp3.** { *; }

# Coil
-keep class coil.** { *; }

# Markwon
-keep class io.github.markwon.** { *; }

# WebKit
-keep class android.webkit.** { *; }

# Keep Parcelable
-keep class * implements android.os.Parcelable { *; }

# Keep Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Application class
-keep class com.amrstudio.AmrStudioApplication { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep Composables
-keep @androidx.compose.runtime.Composable class * { *; }

# Keep Hilt Entry Points
-keep @dagger.hilt.EntryPoint interface *