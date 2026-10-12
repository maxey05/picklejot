# --- Room ------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# --- kotlinx.serialization -------------------------------------------------
# The compiler plugin generates a $$serializer for each @Serializable class and
# reaches it reflectively through the companion.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
# Tightened at RFC-002 (AC-1.10 deferred this until real @Serializable classes existed).
# com.maxeydev.picklejot.data.profile and com.maxeydev.picklejot.data.backup hold the
# @Serializable classes, so the global wildcards above are narrowed to them. Widen this if
# another package gains @Serializable.
-keepclassmembers class com.maxeydev.picklejot.data.profile.**$$serializer { *; }
-keepclasseswithmembers class com.maxeydev.picklejot.data.profile.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class com.maxeydev.picklejot.data.profile.**
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-keepclassmembers class com.maxeydev.picklejot.data.backup.**$$serializer { *; }
-keepclasseswithmembers class com.maxeydev.picklejot.data.backup.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class com.maxeydev.picklejot.data.backup.**
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# --- WorkManager -----------------------------------------------------------
-keepnames class com.maxeydev.picklejot.data.backup.ExportPromptWorker

# --- Play Billing ----------------------------------------------------------
# Billing responses are parsed from JSON into library types; keep the surface.
-keep class com.android.billingclient.api.** { *; }

# --- R93 — logging is stripped in release ----------------------------------
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
