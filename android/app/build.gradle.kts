import java.util.zip.ZipFile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing comes from the environment or from Gradle properties (-P on the command line, or ~/.gradle/gradle.properties), never
// from a file in this repository. Without all of them the release build is made unsigned (it cannot be installed) rather than
// signed with a debug key.
fun signingValue(name: String): String? = (System.getenv(name) ?: providers.gradleProperty(name).orNull)?.takeIf { it.isNotBlank() }
val releaseKeystore = signingValue("COUCOU_KEYSTORE_FILE")
val releaseStorePassword = signingValue("COUCOU_KEYSTORE_PASSWORD")
val releaseKeyAlias = signingValue("COUCOU_KEY_ALIAS")
val releaseKeyPassword = signingValue("COUCOU_KEY_PASSWORD") ?: releaseStorePassword

android {
    namespace = "com.coucou.android"
    compileSdk = 36
    defaultConfig {
        applicationId = "app.coumpanion.android"
        minSdk = 30
        targetSdk = 36
        versionCode = 11
        versionName = "0.6.1"
        manifestPlaceholders["launcherLabel"] = "@string/app_name"
        // The app's languages (English and the translations in i18n/translations); the libraries' other languages are left out of the app.
        resourceConfigurations += listOf("en", "fr", "es", "pt-rBR", "b+zh+Hans", "hi", "bn", "ru", "in", "ar", "tr", "de", "ja", "ko", "b+zh+Hant", "th", "it")
    }
    flavorDimensions += "edition"
    productFlavors {
        create("standard") {
            dimension = "edition"
            applicationIdSuffix = ".standard"
            manifestPlaceholders["launcherLabel"] = "@string/standard_launcher_label"
            buildConfigField("boolean", "NOTIFICATION_WATCH", "false")
        }
        create("full") {
            dimension = "edition"
            buildConfigField("boolean", "NOTIFICATION_WATCH", "true")
        }
    }
    signingConfigs {
        val keystore = releaseKeystore
        val storePass = releaseStorePassword
        val alias = releaseKeyAlias
        if (keystore != null && storePass != null && alias != null) {
            create("release") {
                storeFile = file(keystore)
                storePassword = storePass
                keyAlias = alias
                keyPassword = releaseKeyPassword
            }
        }
    }
    buildTypes {
        release {
            // Shrink, obfuscate and optimise with R8; unused resources go too. The rules (and why there are so few) are in proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") // null: unsigned
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    sourceSets["main"].java.srcDirs("src/main/kotlin")
    sourceSets["full"].java.srcDirs("src/full/kotlin")
    sourceSets["debug"].java.srcDirs("src/debug/kotlin") // the Widgets preview receiver: debug builds only
    sourceSets["test"].java.srcDirs("src/test/kotlin")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    // Scanning the pairing QR code. CameraX (androidx) shows the camera; zxing-core (Apache 2.0, pure Java) reads the
    // code. Neither talks to any server; the reasoning is in the commit message.
    val cameraX = "1.4.2" // Its native libraries support 16 KB Android devices.
    implementation("androidx.camera:camera-core:$cameraX")
    implementation("androidx.camera:camera-camera2:$cameraX")
    implementation("androidx.camera:camera-lifecycle:$cameraX")
    implementation("androidx.camera:camera-view:$cameraX")
    implementation("com.google.zxing:core:3.5.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303") // android.jar stubs org.json in JVM tests
}

// Proves the release build in CI without a workflow change: CI runs `lint`, and lint now needs a release build that R8 has processed and
// whose code no longer carries the debug logs (the Log calls are removed by the rules, so their tags and messages must not be in the dex).
val aapt2 = androidComponents.sdkComponents.aapt2.flatMap { it.executable }
val verifyReleaseNoLogs by tasks.registering {
    group = "verification"
    description = "Verifies packaged service declarations and the removal of debug logs in both release APKs."
    dependsOn("assembleRelease")
    val apkDir = layout.buildDirectory.dir("outputs/apk")
    doLast {
        val leftovers = listOf("CoumpanionOverlay", "CoumpanionLaunch", "CoumpanionDiscovery", "CoumpanionIsland", "island failed", "cannot browse", "ignored an early tap")
        for (edition in listOf("standard", "full")) {
            val apk = apkDir.get().dir("$edition/release").asFile.listFiles { file -> file.extension == "apk" }?.singleOrNull()
            checkNotNull(apk) { "expected one $edition release APK" }
            ZipFile(apk).use { zip ->
                for (entry in zip.entries().asSequence().filter { it.name.endsWith(".dex") }) {
                    val text = String(zip.getInputStream(entry).readBytes(), Charsets.ISO_8859_1)
                    for (word in leftovers) check(!text.contains(word)) { "'$word' is still in $edition ${entry.name}: the debug logs were not stripped" }
                }
            }
            val manifest = providers.exec {
                commandLine(aapt2.get().asFile, "dump", "xmltree", "--file", "AndroidManifest.xml", apk)
            }.standardOutput.asText.get()
            val listenerPermission = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
            check((listenerPermission in manifest) == (edition == "full")) { "unexpected notification listener declaration in $edition" }
            val linkService = manifest.lineSequence()
                .dropWhile { "\"com.coucou.android.app.LinkService\"" !in it }
                .drop(1)
                .takeWhile { it.trimStart().startsWith("A: ") }
            check(linkService.any { "android:foregroundServiceType" in it && it.endsWith("=0x40000000") }) {
                "LinkService must declare the specialUse foreground service type in $edition"
            }
        }
    }
}
tasks.matching { it.name == "lint" }.configureEach { dependsOn(verifyReleaseNoLogs) }
