import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Snimke ekrana iz @Preview funkcija: recordRoborazziDebug / verifyRoborazziDebug
    id("io.github.takahirom.roborazzi") version "1.46.1"
}

roborazzi {
    // Referentne slike su u repozitoriju, pa verify uspoređuje s odobrenim stanjem.
    outputDir.set(file("src/test/snapshots"))
}

// BASE_URL backenda čita se iz local.properties (ključ macromenza.baseUrl), zadano je emulator -> laptop
val localProperties = Properties().apply {
    val datoteka = rootProject.file("local.properties")
    if (datoteka.exists()) datoteka.inputStream().use { load(it) }
}
val baseUrl: String = localProperties.getProperty("macromenza.baseUrl") ?: "http://10.0.2.2:8080/"

android {
    namespace = "com.niko.macromenza"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.niko.macromenza"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        // Robolectric (Application, Patterns, DataStore, Bitmap) treba resurse aplikacije.
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    // android.jar u JVM testovima ima samo prazne org.json metode; stvarna biblioteka ih zamjenjuje.
    testImplementation("org.json:json:20240303")
    // Provjera da JSON iz backenda ne ostavlja null u ne-nullable poljima (Gson zaobilazi Kotlin null-safety).
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:2.0.21")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.46.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.46.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose-preview-scanner-support:1.46.1")
    testImplementation("io.github.sergio-sastre.ComposablePreviewScanner:android:0.6.1")
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")

    implementation("androidx.navigation:navigation-compose:2.9.0")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.datastore:datastore-preferences:1.1.7")

    implementation("androidx.exifinterface:exifinterface:1.3.7")
    // Retrofit 2.11 vuče OkHttp 3.x, a za multipart i timeout koristi se Kotlin API iz 4.x.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

}