import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
}

data class ReleaseSigningConfig(
    val storeFilePath: String?,
    val storePassword: String?,
    val keyAlias: String?,
    val keyPassword: String?
) {
    val missingKeys: List<String>
        get() = buildList {
            if (storeFilePath.isNullOrBlank()) add("release.storeFile")
            if (storePassword.isNullOrBlank()) add("release.storePassword")
            if (keyAlias.isNullOrBlank()) add("release.keyAlias")
            if (keyPassword.isNullOrBlank()) add("release.keyPassword")
        }

    val isConfigured: Boolean
        get() = missingKeys.isEmpty()
}

fun loadOptionalProperties(path: String): Properties {
    val propertiesFile = rootProject.file(path)
    return Properties().apply {
        if (propertiesFile.exists()) {
            propertiesFile.inputStream().use(::load)
        }
    }
}

fun readSigningValue(
    properties: Properties,
    propertyName: String,
    envName: String
): String? {
    return providers.gradleProperty(propertyName).orNull
        ?: properties.getProperty(propertyName)
        ?: System.getenv(envName)
}

val keystoreProperties = loadOptionalProperties("keystore.properties")
val releaseSigning = ReleaseSigningConfig(
    storeFilePath = readSigningValue(
        keystoreProperties,
        "release.storeFile",
        "BRAINCLEAN_RELEASE_STORE_FILE"
    ),
    storePassword = readSigningValue(
        keystoreProperties,
        "release.storePassword",
        "BRAINCLEAN_RELEASE_STORE_PASSWORD"
    ),
    keyAlias = readSigningValue(
        keystoreProperties,
        "release.keyAlias",
        "BRAINCLEAN_RELEASE_KEY_ALIAS"
    ),
    keyPassword = readSigningValue(
        keystoreProperties,
        "release.keyPassword",
        "BRAINCLEAN_RELEASE_KEY_PASSWORD"
    )
)
val releaseStoreFile = releaseSigning.storeFilePath?.let { rootProject.file(it) }

android {
    namespace = "com.example.brainclean"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.brainclean"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (releaseSigning.isConfigured) {
                storeFile = releaseStoreFile
                storePassword = releaseSigning.storePassword
                keyAlias = releaseSigning.keyAlias
                keyPassword = releaseSigning.keyPassword
            }
        }
    }

    buildTypes {
        release {
            if (releaseSigning.isConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        animationsDisabled = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

gradle.taskGraph.whenReady {
    val requiresSignedRelease = allTasks.any { task ->
        task.name in setOf(
            "assembleRelease",
            "bundleRelease",
            "packageRelease",
            "packageReleaseBundle",
            "signReleaseBundle"
        )
    }

    if (requiresSignedRelease) {
        check(releaseSigning.isConfigured) {
            val missingKeys = releaseSigning.missingKeys.joinToString(", ")
            "Missing release signing configuration. Create keystore.properties from keystore.properties.example " +
                "or set the BRAINCLEAN_RELEASE_* environment variables. Missing: $missingKeys"
        }

        check(releaseStoreFile?.exists() == true) {
            "Release keystore file not found at '${releaseSigning.storeFilePath}'. " +
                "Update release.storeFile or BRAINCLEAN_RELEASE_STORE_FILE."
        }
    }
}
