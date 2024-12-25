plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-kapt") // Для Room
}

android {
    namespace = "com.shiroma.emfreader"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.shiroma.emfreader"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        viewBinding = true // Включение ViewBinding
    }

    kotlinOptions {
        jvmTarget = "17" // Или "17", если вы используете Java 17
    }

    testOptions{
        unitTests.isIncludeAndroidResources = false
    }

    tasks.withType<Test> {
        // Отключаем юнит-тесты
        enabled = false
    }
}

kotlin {
    jvmToolchain(17) // Убедитесь, что все компилируется под JVM 17
}

kapt{
    correctErrorTypes = true
    useBuildCache = false
}



dependencies {
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("com.github.PhilJay:MPAndroidChart:v3.0.0")
    implementation("androidx.room:room-ktx:2.6.1") // Добавляем эту строку
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}