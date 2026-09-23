plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ove.edit"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ove.edit"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")
    
    val composeBom = platform("androidx.compose:compose-bom:2023.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Media3 (Transformer & ExoPlayer)
    val media3Version = "1.2.0"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-transformer:$media3Version")
    implementation("androidx.media3:media3-effect:$media3Version")

    // MediaPipe
    implementation("com.google.mediapipe:tasks-vision:0.10.14")
    implementation("com.google.mediapipe:tasks-text:0.10.14")
    implementation("com.google.mediapipe:tasks-audio:0.10.14")

    // ONNX Runtime (CPU)
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.16.3")

    // ML Kit Translation
    implementation("com.google.mlkit:translate:17.0.2")
}
dependencies {
    implementation("androidx.navigation:navigation-compose:2.7.4")
}
dependencies {
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
}
dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
}
dependencies {
    implementation("androidx.compose.material:material-icons-extended")
}
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
}
