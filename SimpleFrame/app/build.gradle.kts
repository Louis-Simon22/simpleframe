plugins {
    id("com.android.application")
}

android {
    namespace = "com.louissimonmcnicoll.simpleframe"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.louissimonmcnicoll.simpleframe"
        minSdk = 23
        targetSdk = 34
        versionCode = 4
        versionName = "2.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}