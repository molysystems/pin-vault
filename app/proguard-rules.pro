# PinVault ProGuard Rules

# Keep Room entities
-keep class com.molysystems.pinvault.data.model.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Room database classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Keep Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# AndroidKeyStore
-keep class android.security.keystore.** { *; }

# Suppress warnings for missing classes
-dontwarn java.lang.invoke.**
