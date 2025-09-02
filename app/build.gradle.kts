// *** الإصلاح الحاسم هنا: إضافة جمل import اللازمة ***
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

fun generateVersionCode(): Int {
    return (System.currentTimeMillis() / 1000).toInt()
}

fun generateVersionName(): String {
    // الآن، سيعمل هذا السطر بشكل صحيح بسبب جمل import أعلاه
    val date = SimpleDateFormat("yyyy.MM.dd.HHmm", Locale.getDefault()).format(Date())
    return "1.0.$date"
}

android {
    namespace = "com.example.manusagentapp"
    compileSdk = 34

    // ... (كل إعدادات android الأخرى لا تتغير) ...
    signingConfigs {
        create("release") {
            val keyAlias = System.getenv("MY_SIGNING_KEY_ALIAS")
            val keyPassword = System.getenv("MY_SIGNING_KEY_PASSWORD")
            val storePassword = System.getenv("MY_SIGNING_KEY_PASSWORD")
            val storeFileBase64 = System.getenv("MY_SIGNING_KEY_BASE64")

            if (storeFileBase64 != null) {
                val signingKeyFile = File(rootProject.projectDir, "signing_key.keystore")
                signingKeyFile.writeBytes(Base64.getDecoder().decode(storeFileBase64))
                this.storeFile = signingKeyFile
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.example.manusagentapp"
        minSdk = 24
        targetSdk = 34
        versionCode = generateVersionCode()
        versionName = generateVersionName()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.13" 
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // === تجربة العزل: سنبقي على الأساسيات فقط ===
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    
    implementation(platform("androidx.compose:compose-bom:2024.06.00")) 
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // === تم تعطيل هذه المكتبات مؤقتًا لتحديد مصدر المشكلة ===
    // implementation("androidx.appcompat:appcompat:1.7.0")
    // implementation("com.microsoft.onnxruntime:onnxruntime-android:1.18.0")
    
    // === تم تعطيل تبعيات الاختبار مؤقتًا ===
    // testImplementation("junit:junit:4.13.2")
    // androidTestImplementation("androidx.test.ext:junit:1.1.5")
    // androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    // androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    // androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
