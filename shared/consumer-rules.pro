# Keep all exported shared classes and generated Compose resources
-keep class com.worldwidewaves.shared.** { *; }
-keep class com.worldwidewaves.shared.generated.resources.** { *; }

# Firebase Crashlytics - Required for CrashlyticsLogger
-keep class com.google.firebase.crashlytics.** { *; }
-keep class com.google.firebase.ktx.** { *; }
-keepattributes SourceFile,LineNumberTable
