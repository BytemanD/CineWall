buildscript {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.7.3")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.20")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.0.20")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.0.20-1.0.25")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.8.0")
    }
}
tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
