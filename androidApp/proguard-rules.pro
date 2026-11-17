# R8 config. kotlinx.serialization, Ktor, OkHttp and Compose ship their rules in the libraries.
# JVM classes the Ktor client references but Android lacks (not used on any execution path)
-dontwarn java.lang.management.**
-dontwarn org.slf4j.**

# Glance finds a widget's placed instances by its class (updateAll, getGlanceIds). R8 would merge the four widget
# classes into one, and then updating one kind redraws every widget as that kind. Keep each class distinct.
-keep,allowobfuscation class * extends androidx.glance.appwidget.GlanceAppWidget
