import java.util.Properties

plugins { id("com.android.application"); id("org.jetbrains.kotlin.plugin.compose"); id("com.google.devtools.ksp") }

val uploadProperties = Properties().apply {
 val file = rootProject.file("release/upload-key.properties")
 if (file.exists()) file.inputStream().use { load(it) }
}


android {
 namespace = "bg.newlook"; compileSdk = 37
 defaultConfig { applicationId = "bg.newlook.organizer"; minSdk = 26; targetSdk = 36; versionCode = 2; versionName = "1.1.0"; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"; ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64") } }
 // Private local debug key is optional; clean CI uses the Android debug keystore.
 if (rootProject.file("debug/newlook-debug.keystore").exists()) {
  signingConfigs.getByName("debug") { storeFile = rootProject.file("debug/newlook-debug.keystore"); storePassword = "android"; keyAlias = "androiddebugkey"; keyPassword = "android" }
 }
 signingConfigs.create("release") {
  if (uploadProperties.isNotEmpty()) {
   storeFile = rootProject.file(uploadProperties.getProperty("storeFile"))
   storePassword = uploadProperties.getProperty("storePassword")
   keyAlias = uploadProperties.getProperty("keyAlias")
   keyPassword = uploadProperties.getProperty("keyPassword")
  }
 }
 buildTypes.getByName("release") {
  signingConfig = signingConfigs.getByName("release")
  isMinifyEnabled = false
 }
 splits { abi { isEnable = false } }
 bundle { language { enableSplit = false } }
 buildFeatures { compose = true; buildConfig = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 testOptions { unitTests.isReturnDefaultValues = true }
}
kotlin { jvmToolchain(17) }
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
 implementation(platform("androidx.compose:compose-bom:2026.09.00"))
 implementation("androidx.activity:activity-compose:1.13.0")
 implementation("androidx.fragment:fragment:1.9.1")
 implementation("androidx.core:core-ktx:1.19.1")
 implementation("androidx.core:core-splashscreen:1.2.0")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.navigation:navigation-compose:2.10.2")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
 implementation("androidx.datastore:datastore-preferences:1.3.0")
 implementation("androidx.room:room-runtime:2.8.5")
 implementation("androidx.room:room-ktx:2.8.5")
 ksp("androidx.room:room-compiler:2.8.5")
 implementation("androidx.work:work-runtime-ktx:2.12.0")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
 implementation("com.google.code.gson:gson:2.15.0")
 implementation("com.airbnb.android:lottie-compose:6.7.1")
 testImplementation("junit:junit:4.13.2")
 androidTestImplementation(platform("androidx.compose:compose-bom:2026.09.00"))
 androidTestImplementation("androidx.compose.ui:ui-test-junit4")
 androidTestImplementation("androidx.test:runner:1.7.0")
 androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
 androidTestImplementation("androidx.test.ext:junit:1.3.0")
 androidTestImplementation("androidx.room:room-testing:2.8.5")
}
