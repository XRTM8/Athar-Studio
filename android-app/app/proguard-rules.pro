# Proguard rules for Athar Studio
-keep class app.xrtm8.athar.core.** { *; }
-keep class app.xrtm8.athar.model.** { *; }
-keep class app.xrtm8.athar.project.** { *; }
-keep class app.xrtm8.athar.typography.** { *; }
-keep class app.xrtm8.athar.export.** { *; }
-keep class app.xrtm8.athar.ui.** { *; }
-keep class app.xrtm8.athar.updater.** { *; }

# Preserve Compose runtime annotations
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Coroutines & Lifecycle warnings suppression
-dontwarn kotlinx.coroutines.**

# Keep line numbers for stack traces in release builds
-keepattributes SourceFile,LineNumberTable,*Annotation*
