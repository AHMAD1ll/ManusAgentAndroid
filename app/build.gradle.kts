plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Function to generate a unique version code based on the current time
fun generateVersionCode(): Int {
    return (System.currentTimeMillis() / 1000).toInt()
}

// Function to generate a user-friendly version name
fun generateVersionName(): String {
    val date = java.text.SimpleDateFormat("yyyy.MM.dd.HHmm", java.util.Locale.getDefault()).format(java.util.Date())
    return "1.0.$date"
}


android {
    namespace = "com.example.manusagentapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.manusagentapp"
        minSdk = 24
        targetSdk = 34
        
        // --- THE FIX IS HERE ---
        // Automatically set a new, unique version code and name for every build
        versionCode = generateVersionCode()
        versionName = generateVersionName()
        // --- END OF FIX ---

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
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.05.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
