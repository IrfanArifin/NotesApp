plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.notesapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.notesapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.database) // Firebase Realtime Database
    implementation(libs.firebase.auth)     // Firebase Authentication
    implementation("androidx.recyclerview:recyclerview:1.3.2") // RecyclerView
    implementation("androidx.cardview:cardview:1.0.0") // CardView untuk kartu profil
    implementation("com.github.bumptech.glide:glide:4.16.0") // Glide untuk memuat foto profil
    implementation("com.google.firebase:firebase-storage:20.3.0") // Firebase Storage untuk menyimpan foto
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}