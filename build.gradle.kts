// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // الإصلاح: تحديث إصدار إضافة أندرويد ليتوافق مع أحدث مكتبات Compose
    id("com.android.application") version "8.4.1" apply false 
    // الإصلاح: تحديث إصدار Kotlin ليتوافق مع إضافة أندرويد الجديدة
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false 
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23" apply false
}
