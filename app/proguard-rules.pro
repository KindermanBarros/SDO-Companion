# SDO Companion - R8 & ProGuard Optimization Rules

# Keep line numbers and source file attributes for readable stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve data models for Firebase Firestore reflection (toObject) and Room
-keep class com.kinderman.sdo.data.** { *; }
-keepclassmembers class com.kinderman.sdo.data.** { *; }
-keep class com.kinderman.sdo.domain.model.** { *; }
-keepclassmembers class com.kinderman.sdo.domain.model.** { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
