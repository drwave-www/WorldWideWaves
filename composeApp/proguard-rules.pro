# Keep all shared module classes referenced from app
-keep class com.worldwidewaves.shared.** { *; }
-keep class com.worldwidewaves.shared.generated.resources.** { *; }

# Firebase Crashlytics KTX - Required by CrashlyticsLogger in shared module
-keep class com.google.firebase.crashlytics.ktx.** { *; }
-keep class com.google.firebase.ktx.** { *; }
-keep class com.google.firebase.crashlytics.** { *; }
-keepattributes SourceFile,LineNumberTable

# Allow R8 to proceed despite missing Firebase KTX classes
# These classes are provided at runtime by firebase-common-ktx and firebase-crashlytics-ktx dependencies
-dontwarn com.google.firebase.crashlytics.ktx.FirebaseCrashlyticsKt
-dontwarn com.google.firebase.ktx.Firebase

# Ignore test-only classes that might be seen during shrinking
-dontwarn org.junit.**
-dontwarn org.junit.jupiter.**
-dontwarn org.apiguardian.api.**

# j2objc annotations referenced by Guava/WorkManager
-dontwarn com.google.j2objc.annotations.**
-keep class com.google.j2objc.annotations.** { *; }
