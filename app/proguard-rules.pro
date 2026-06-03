# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Administrator\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard/index.html

# Keep Room related classes if necessary (though Room often handles its own)
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}
