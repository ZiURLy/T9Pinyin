plugins { id("com.android.application") }

android {
    namespace = "com.ziur.t9pinyin"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.ziur.t9pinyin"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "0.3"
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    lint { checkReleaseBuilds = false }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
}
