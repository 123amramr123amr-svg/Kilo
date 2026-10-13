plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("kotlinx-serialization")
}

android {
    namespace = "com.amrstudio"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.amrstudio"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-Xopt-in=kotlin.RequiresOptIn")
    }

    buildFeatures {
        compose = true
        viewBinding = true
        dataBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packagingOptions {
        resources.excludes.add("META-INF/*")
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
}

dependencies {
    // Core Android
    val coreKtx = "1.13.1"
    val activityCompose = "1.9.0"
    val lifecycleVersion = "2.8.0"
    val roomVersion = "2.6.1"
    val datastoreVersion = "1.1.1"
    val hiltVersion = "2.48.1"
    val workVersion = "2.9.0"
    val navigationVersion = "2.7.7"
    val pagingVersion = "3.2.1"
    val retrofitVersion = "2.11.0"
    val okhttpVersion = "4.12.0"
    val kotlinxCoroutinesVersion = "1.7.3"
    val kotlinxSerializationVersion = "1.6.2"
    val coilVersion = "2.6.0"
    val material3Version = "1.3.0"
    val markdownVersion = "0.7.0"
    val webkitVersion = "1.10.0"

    implementation("androidx.core:core-ktx:$coreKtx")
    implementation("androidx.activity:activity-compose:$activityCompose")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.datastore:datastore-preferences:$datastoreVersion")
    implementation("androidx.datastore:datastore-preferences-rxjava3:$datastoreVersion")
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    ksp("com.google.dagger:hilt-compiler:$hiltVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.work:work-runtime-ktx:$workVersion")
    implementation("androidx.navigation:navigation-compose:$navigationVersion")
    implementation("androidx.paging:paging-compose:$pagingVersion")
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-moshi:$retrofitVersion")
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$kotlinxCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    implementation("io.coil-kt:coil-compose:$coilVersion")
    implementation("androidx.compose.material3:material3:$material3Version")
    implementation("androidx.compose.material3:material3-window-size-class:$material3Version")
    implementation("androidx.compose.ui:ui-graphics:$material3Version")
    implementation("androidx.webkit:webkit:$webkitVersion")
    implementation("com.robertlevonyan.views:markdown-view-compose:1.0.0")
    implementation("io.github.markwon:markwon:4.6.3")
    implementation("io.github.markwon:markwon-ext-strikethrough:4.6.3")
    implementation("io.github.markwon:markwon-ext-tables:4.6.3")
    implementation("io.github.markwon:markwon-ext-tasklist:4.6.3")
    implementation("io.github.markwon:markwon-image-coil:4.6.3")
    implementation("io.github.markwon:markwon-recyclerview:4.6.3")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.biometric:biometric-ktx:1.2.0-alpha04")
    implementation("androidx.security:security-crypto:1.1.0-alpha05")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesVersion")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.5")
    androidTestImplementation("androidx.compose.ui:ui-test-manifest:1.6.5")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.5")
    debugImplementation("androidx.compose.ui:ui-tooling-preview:1.6.5")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("hilt.disableAggregatingComponents", "true")
}