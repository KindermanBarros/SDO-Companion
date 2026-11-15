# SDO Companion - R8 & ProGuard Optimization Rules

# Keep line numbers, generic type signatures, and annotations for Firestore/Gson/Room reflection
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,AnnotationDefault,*Annotation*
-renamesourcefileattribute SourceFile

# Preserve data models for Firebase Firestore reflection (toObject) and Room
-keep class com.kinderman.sdo.data.** { *; }
-keepclassmembers class com.kinderman.sdo.data.** { *; }
-keep class com.kinderman.sdo.domain.model.** { *; }
-keepclassmembers class com.kinderman.sdo.domain.model.** { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
