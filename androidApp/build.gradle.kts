import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.example.dovashiapp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.dovashiapp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        // Never committed: from the untracked local.properties, else the environment (doc 15).
        val localProperties = Properties().apply {
            rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
        }
        val openAiApiKey = localProperties.getProperty("OPENAI_API_KEY")?.takeIf { it.isNotBlank() }
            ?: System.getenv("OPENAI_API_KEY")?.takeIf { it.isNotBlank() }
            ?: ""
        // Keys are printable ASCII: strip pasted quotes and anything invisible so the header (and BuildConfig) is valid.
        val escapedKey = openAiApiKey.trim().removeSurrounding("\"").removeSurrounding("'").filter { it in '!'..'~' }
            .replace("\\", "\\\\").replace("\"", "\\\"")
        buildConfigField("String", "OPENAI_API_KEY", "\"$escapedKey\"")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}