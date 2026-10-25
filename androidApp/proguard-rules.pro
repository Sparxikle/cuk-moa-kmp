# R8 config. kotlinx.serialization, Ktor, OkHttp and Compose ship their rules in the libraries.
# JVM classes the Ktor client references but Android lacks (not used on any execution path)
-dontwarn java.lang.management.**
-dontwarn org.slf4j.**
