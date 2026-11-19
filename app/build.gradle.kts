plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.cart3r.aimlinedemo"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.cart3r.aimlinedemo.v6"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "6.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    testImplementation("junit:junit:4.13.2")
}
