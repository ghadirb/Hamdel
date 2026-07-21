import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

val secretsProperties = Properties().apply {
    val secretsFile = rootProject.file("secrets.properties")
    if (secretsFile.exists()) {
        secretsFile.inputStream().use { load(it) }
    }
}

val billingProperties = Properties().apply {
    val billingFile = rootProject.file("billing.properties")
    if (billingFile.exists()) billingFile.inputStream().use { load(it) }
}
val serverProperties = Properties().apply {
    val serverFile = rootProject.file("server.properties")
    if (serverFile.exists()) serverFile.inputStream().use { load(it) }
}
fun quotedBuildConfig(value: String): String = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val releaseSigningProperties = Properties().apply {
    val signingFile = rootProject.file("release-signing.properties")
    if (signingFile.exists()) signingFile.inputStream().use { load(it) }
}
val releaseStoreFile = releaseSigningProperties.getProperty("RELEASE_STORE_FILE")
val releaseStorePassword = releaseSigningProperties.getProperty("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = releaseSigningProperties.getProperty("RELEASE_KEY_ALIAS")
val releaseKeyPassword = releaseSigningProperties.getProperty("RELEASE_KEY_PASSWORD")
val hasReleaseSigning = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { !it.isNullOrBlank() }

android {
    namespace = "com.hamdel.ai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hamdel.ai"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Password used to decrypt the remote keys.txt (see encrypt_keys.py).
        // Defaults to the password used for the published encrypted bundle so public
        // builds can use the online providers without committing raw API keys.
        buildConfigField(
            "String",
            "KEYS_DECRYPT_PASSWORD",
            "\"${secretsProperties.getProperty("KEYS_DECRYPT_PASSWORD", "12345")}\""
        )
        buildConfigField(
            "String",
            "HAMDEL_SERVER_BASE_URL",
            quotedBuildConfig(serverProperties.getProperty("HAMDEL_SERVER_BASE_URL", ""))
        )
        buildConfigField(
            "String",
            "HAMDEL_SERVER_API_KEY",
            quotedBuildConfig(serverProperties.getProperty("HAMDEL_SERVER_API_KEY", ""))
        )
    }

    flavorDimensions += "store"
    productFlavors {
        create("bazaar") {
            dimension = "store"
            manifestPlaceholders["marketApplicationId"] = "com.farsitel.bazaar"
            manifestPlaceholders["marketBindAddress"] = "ir.cafebazaar.pardakht.InAppBillingService.BIND"
            manifestPlaceholders["marketPermission"] = "com.farsitel.bazaar.permission.PAY_THROUGH_BAZAAR"
            buildConfigField("String", "STORE_ID", "\"bazaar\"")
            buildConfigField("String", "IAB_PUBLIC_KEY", quotedBuildConfig(billingProperties.getProperty("BAZAAR_IAB_PUBLIC_KEY", "")))
            // Existing Bazaar products were created with these IDs. Keep them configurable
            // so the two store catalogs never need to share product IDs.
            buildConfigField("String", "MONTHLY_SKU", quotedBuildConfig(billingProperties.getProperty("BAZAAR_MONTHLY_SKU", "com.hamdel.ai")))
            buildConfigField("String", "YEARLY_SKU", quotedBuildConfig(billingProperties.getProperty("BAZAAR_YEARLY_SKU", "hamdel_yearly")))
        }
        create("myket") {
            dimension = "store"
            manifestPlaceholders["marketApplicationId"] = "ir.mservices.market"
            manifestPlaceholders["marketBindAddress"] = "ir.mservices.market.InAppBillingService.BIND"
            manifestPlaceholders["marketPermission"] = "ir.mservices.market.BILLING"
            buildConfigField("String", "STORE_ID", "\"myket\"")
            buildConfigField("String", "IAB_PUBLIC_KEY", quotedBuildConfig(billingProperties.getProperty("MYKET_IAB_PUBLIC_KEY", "")))
            buildConfigField("String", "MONTHLY_SKU", quotedBuildConfig(billingProperties.getProperty("MYKET_MONTHLY_SKU", "com.hamdel.ai")))
            buildConfigField("String", "YEARLY_SKU", quotedBuildConfig(billingProperties.getProperty("MYKET_YEARLY_SKU", "hamdel_yearly")))
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

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes.named("release") {
        if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.github.myketstore:myket-billing-client:1.19")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
