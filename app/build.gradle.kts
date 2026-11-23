import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("androidx.navigation.safeargs")
}

// 刮削接口密钥：环境变量 > 项目根目录 cinewall.properties > 空字符串。
// 构建期写入 BuildConfig，安装后的应用开箱即用，无需在 UI 里填写。
val cinewallSecrets = Properties().apply {
    val file = rootProject.file("cinewall.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun secret(name: String): String {
    val fromEnv = System.getenv(name)?.trim().orEmpty()
    val fromFile = cinewallSecrets.getProperty(name, "").trim()
    return (fromEnv.ifEmpty { fromFile })
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
}

android {
    namespace = "com.cinewall.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cinewall.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "TMDB_API_KEY", "\"${secret("TMDB_API_KEY")}\"")
        buildConfigField("String", "DOUBAN_API_KEY", "\"${secret("DOUBAN_API_KEY")}\"")
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
        buildConfig = true
    }

    packaging {
        resources {
            excludes.add("/META-INF/AL2.0")
            excludes.add("/META-INF/LGPL2.1")
        }
    }

    sourceSets {
        getByName("main") {
            res.srcDirs("src/main/res")
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.00")
    val tvComposeVersion = "1.0.0"
    val roomVersion = "2.6.1"
    val exoVersion = "2.18.7"
    val coilVersion = "2.6.0"
    val lifecycleVersion = "2.8.2"

    implementation(composeBom)

    // AndroidX Core & Activity Compose
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.fragment:fragment-ktx:1.8.2")

    // Compose for TV
    implementation("androidx.tv:tv-material:$tvComposeVersion")
    implementation("androidx.tv:tv-foundation:$tvComposeVersion")

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.runtime:runtime-livedata")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // Room
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // ExoPlayer
    implementation("com.google.android.exoplayer:exoplayer:$exoVersion")
    implementation("com.google.android.exoplayer:extension-okhttp:$exoVersion")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.materialkolor:material-kolor:2.1.1")

    // Coil image loading
    implementation("io.coil-kt:coil:$coilVersion")
    implementation("io.coil-kt:coil-compose:$coilVersion")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")

    // TV Leanback (for leanback utilities if needed)
    implementation("androidx.leanback:leanback:1.2.0")

    // MediaStore / Storage helpers
    implementation("androidx.media:media:1.7.0")
    implementation("androidx.documentfile:documentfile:1.0.1")

    // Test
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }
}
