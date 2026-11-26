# TrackPulse R8 / ProGuard optimization rules

# Preserve Android foreground service, restart receiver & model enums used with valueOf()
-keep class com.example.service.LocationTrackingService { *; }
-keep class com.example.service.TrackingRestartReceiver { *; }
-keepclassmembers enum com.example.data.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep OkHttp optional platform warnings clean during R8 shrinking
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**
