# Room ships its own consumer rules; the entities are kept for the generated DAOs.
-keep class de.meintraining.app.data.** { *; }

# Coil
-dontwarn okhttp3.**
-dontwarn okio.**

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
