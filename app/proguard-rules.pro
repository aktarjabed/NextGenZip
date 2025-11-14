# Keep JNI native bridge
-keep class com.aktarjabed.nextgenzip.ai.NativeBridge { *; }

# Keep AIManager flows
-keepclassmembers class com.aktarjabed.nextgenzip.ai.** { *; }

# Keep archive libraries
-keep class net.lingala.zip4j.** { *; }
-keep class org.apache.commons.compress.** { *; }
-keep class com.github.junrar.** { *; }

-dontwarn org.apache.commons.compress.**
-dontwarn com.github.junrar.**

# Keep native methods
-keepclasseswithmembernames class * {
native
}

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
