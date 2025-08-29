# Keep all shared module classes referenced from app
-keep class com.worldwidewaves.shared.** { *; }
-keep class com.worldwidewaves.shared.generated.resources.** { *; }

# Ignore test-only classes that might be seen during shrinking
-dontwarn org.junit.**
-dontwarn org.junit.jupiter.**
-dontwarn org.apiguardian.api.**

# j2objc annotations referenced by Guava/WorkManager
-dontwarn com.google.j2objc.annotations.**
-keep class com.google.j2objc.annotations.** { *; }
