plugins {
    id("com.android.application")
}

android {
    namespace = "pl.proszczok.pixelspeecheq"
    compileSdk = 37

    defaultConfig {
        applicationId = "pl.proszczok.pixelspeecheq"
        minSdk = 28
        targetSdk = 37
        versionCode = providers.gradleProperty("pixelCenterVersionCode")
            .orElse("31").get().toInt()
        versionName = providers.gradleProperty("pixelCenterVersionName")
            .orElse("1.03").get()
        testInstrumentationRunner =
            "pl.proszczok.pixelspeecheq.ScreenOffGestureInstrumentation"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
