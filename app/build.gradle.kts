import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing: ~/.still/keystore.properties (never in the repo).
val signing = File(System.getProperty("user.home"), ".still/keystore.properties")
val signingProps = Properties().apply { if (signing.exists()) signing.inputStream().use { load(it) } }

// Date of the latest internal security review, the newest docs/security/YYYY-MM-DD.md. Shown in About.
val securityReview: String = rootProject.file("docs/security").listFiles().orEmpty()
    .map { it.name }.filter { Regex("""\d{4}-\d{2}-\d{2}\.md""").matches(it) }
    .maxOrNull()?.removeSuffix(".md") ?: error("No review in docs/security/")

android {
    namespace = "dev.sidali.still"
    compileSdk = 36
    defaultConfig {
        applicationId = "dev.sidali.still"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "SECURITY_REVIEW", "\"$securityReview\"")
    }
    signingConfigs {
        if (signing.exists()) create("release") {
            storeFile = file(signingProps.getProperty("storeFile"))
            storePassword = signingProps.getProperty("storePassword")
            keyAlias = signingProps.getProperty("keyAlias")
            keyPassword = signingProps.getProperty("keyPassword")
        }
    }
    buildTypes {
        release {
            // Reproducible builds: no git commit id baked into the APK.
            vcsInfo.include = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // -Punsigned builds an unsigned APK, used to check that a build is reproducible.
            if (!project.hasProperty("unsigned")) signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    dependenciesInfo { includeInApk = false; includeInBundle = false }
}
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
    val bom = platform("androidx.compose:compose-bom:2025.09.01")
    implementation(bom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    testImplementation("junit:junit:4.13.2")
}
