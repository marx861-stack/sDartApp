import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.regex.Pattern

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.aboutLibraries)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }
    
    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "sDartApp"
            isStatic = true
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.viewmodel.compose)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.aboutlibraries.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)
                implementation(libs.kotlinx.coroutines.android)
                
                // CameraX
                implementation(libs.camera.core)
                implementation(libs.camera.camera2)
                implementation(libs.camera.lifecycle)
                implementation(libs.camera.view)
                


                implementation(libs.java.websocket)
            }
        }
        
        val desktopMain by getting {
            resources.srcDir("src/androidMain/assets")
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.desktop.linux_x64)
                implementation(libs.kotlinx.coroutines.swing)
                implementation("com.github.sarxos:webcam-capture:0.3.12")

                implementation("net.java.dev.jna:jna-platform:5.14.0")
                implementation("org.jetbrains.runtime:jbr-api:1.5.0")
                implementation("javazoom:jlayer:1.0.1")
                implementation("org.jmdns:jmdns:3.5.9")
                implementation(libs.java.websocket)
                implementation("com.microsoft.onnxruntime:onnxruntime:1.18.0")
            }
        }
        
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}

android {
    namespace = "com.sdart.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sdart.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    ndkVersion = "27.0.12077973"

    externalNativeBuild {
        cmake {
            path = file("src/androidMain/cpp/CMakeLists.txt")
            version = "3.22.1+"
        }
    }
}

// Lizenzliste (Einstellungen → Lizenzen): alle Abhängigkeiten inkl. transitiver werden beim Build
// mit Lizenztexten erfasst und als JSON in die Compose-Ressourcen geschrieben.
// Bibliotheken ohne Maven-Artefakt (Modell, native Libs) stehen in composeApp/aboutlibraries/.
aboutLibraries {
    registerAndroidTasks = false
    prettyPrint = true
    configPath = "composeApp/aboutlibraries"
    excludeFields = arrayOf("funding")
    // KMP-Bibliotheken haben je Plattform ein eigenes Artefakt (z.B. -android, -jvm): zu einem Eintrag zusammenführen
    duplicationMode = com.mikepenz.aboutlibraries.plugin.DuplicateMode.MERGE
    duplicationRule = com.mikepenz.aboutlibraries.plugin.DuplicateRule.SIMPLE
    // Nur Test-Abhängigkeiten – werden nicht mit der App ausgeliefert
    exclusionPatterns = listOf(
        "junit:junit",
        "org\\.hamcrest:.*",
        "org\\.jetbrains\\.kotlin:kotlin-test.*",
        "org\\.jetbrains\\.kotlinx:kotlinx-coroutines-test.*"
    ).map { Pattern.compile(it) }
}
// Ausgabepfad der Gesamtliste: aboutLibraries.exportPath in gradle.properties (Fallback für iOS)

// Jede App liefert nur ihre eigenen Abhängigkeiten aus (Android: androidx-Artefakte, Desktop: JetBrains-Ports).
// Deshalb pro Plattform eine eigene Liste, sonst würden dieselben Komponenten doppelt und mit
// Bibliotheken der jeweils anderen Plattform erscheinen. Variante = Gradle-Konfiguration der Plattform.
// Manuelle Einträge (aboutlibraries/), die nur auf einer Plattform ausgeliefert werden, hier ausnehmen.
val licenseListPerPlatform = mapOf(
    "android" to Pair("androidRelease", setOf<String>()),
    "desktop" to Pair("desktop", setOf("com.tencent:ncnn"))
)
val platformLicenseTasks = licenseListPerPlatform.map { (platform, config) ->
    val (gradleVariant, excludedIds) = config
    tasks.register<com.mikepenz.aboutlibraries.plugin.AboutLibrariesTask>(
        "exportLibraryDefinitions${platform.replaceFirstChar { it.uppercase() }}"
    ) {
        description = "Schreibt die Lizenzliste für $platform nach composeResources/files/$platform"
        group = "Build"
        variant = gradleVariant
        resultDirectory = file("src/commonMain/composeResources/files/$platform")
        dependsOn("collectDependencies")
        doLast {
            if (excludedIds.isEmpty()) return@doLast
            val slurper = groovy.json.JsonSlurper()
            @Suppress("UNCHECKED_CAST")
            val json = slurper.parse(combinedLibrariesOutputFile) as MutableMap<String, Any?>
            @Suppress("UNCHECKED_CAST")
            val libs = (json["libraries"] as List<Map<String, Any?>>).filter { it["uniqueId"] !in excludedIds }
            @Suppress("UNCHECKED_CAST")
            val usedLicenses = libs.flatMap { it["licenses"] as? List<String> ?: emptyList() }.toSet()
            @Suppress("UNCHECKED_CAST")
            val licenses = (json["licenses"] as Map<String, Any?>).filterKeys { it in usedLicenses }
            json["libraries"] = libs
            json["licenses"] = licenses
            val generator = groovy.json.JsonGenerator.Options().disableUnicodeEscaping().build()
            combinedLibrariesOutputFile.writeText(
                groovy.json.JsonOutput.prettyPrint(generator.toJson(json)), Charsets.UTF_8
            )
        }
    }
}

// Eigener Lizenztext der App (GPL-3.0): immer direkt aus der LICENSE im Projektordner übernehmen,
// damit der in der App angezeigte Text nie von der veröffentlichten Lizenz abweicht.
val copyAppLicense by tasks.registering(Copy::class) {
    description = "Kopiert die LICENSE der App in die Compose-Ressourcen"
    from(rootProject.file("LICENSE"))
    into("src/commonMain/composeResources/files")
    rename { "app-license.txt" }
}

tasks.matching { it.name.endsWith("ComposeResourcesForCommonMain") || it.name.endsWith("ResourcesForCommonMain") }
    .configureEach {
        dependsOn("exportLibraryDefinitions", copyAppLicense)
        platformLicenseTasks.forEach { dependsOn(it) }
    }

val appVersion = "1.0.0"

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "sDartApp"
            packageVersion = appVersion

            windows {
                iconFile.set(project.file("src/desktopMain/resources/icon.ico"))
                menuGroup = "Game"
            }
            macOS {
                iconFile.set(project.file("src/desktopMain/resources/icon.icns"))
                bundleID = "com.sdart.app"
            }
            linux {
                iconFile.set(project.file("src/desktopMain/resources/icon.png"))
                debPackageVersion = appVersion
                menuGroup = "Game"
                appCategory = "Game"
            }
        }
    }
}

tasks.withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

val packageLinuxUberJar by tasks.registering(Copy::class) {
    group = "compose desktop"
    description = "Packages the Linux x64 executable UberJar."
    dependsOn("packageUberJarForCurrentOS")
    from(layout.buildDirectory.dir("compose/jars"))
    include("sDartApp-windows-x64-$appVersion.jar")
    into(layout.buildDirectory.dir("compose/jars"))
    rename { "sDartApp-linux-x64-$appVersion.jar" }
}

tasks.register("packageLinuxJar") {
    group = "compose desktop"
    description = "Alias for packageLinuxUberJar."
    dependsOn(packageLinuxUberJar)
}

val copyDistributables by tasks.registering(Copy::class) {
    group = "distribution"
    description = "Copies strictly the final 3 release artifacts into root dist/ directory, overwriting older builds."
    
    // 1. Android APK (umbenannt mit Versionsnummer)
    from(layout.buildDirectory.dir("outputs/apk/debug")) {
        include("composeApp-debug.apk")
        rename { "sDartApp-android-$appVersion.apk" }
    }
    // 2. Desktop JARs (ausschließlich Windows und Linux Standalone UberJars)
    from(layout.buildDirectory.dir("compose/jars")) {
        include("sDartApp-windows-x64-$appVersion.jar")
        include("sDartApp-linux-x64-$appVersion.jar")
    }
    // 3. iOS IPAs & Frameworks (sobald auf macOS / CI gebaut)
    from(layout.buildDirectory.dir("outputs/ipa")) {
        include("*.ipa")
        into("ios")
    }
    from(layout.buildDirectory.dir("bin/iosArm64/releaseFramework")) {
        include("**/*")
        into("ios/sDartApp.framework")
    }
    into(rootProject.file("dist"))
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

val uploadToGoogleDrive by tasks.registering(Exec::class) {
    group = "distribution"
    description = "Uploads and syncs all artifacts from dist/ directly to Google Drive under 'Projekte/ Dart app'."
    dependsOn(copyDistributables)
    workingDir = rootProject.projectDir

    if (org.gradle.internal.os.OperatingSystem.current().isWindows) {
        commandLine("cmd", "/c", "tools\\upload_to_drive.bat")
    } else {
        commandLine("pwsh", "-File", "tools/upload_to_drive.ps1")
    }
}

tasks.matching { it.name in listOf("packageDebug", "packageUberJarForCurrentOS", "packageLinuxUberJar") }.configureEach {
    finalizedBy(copyDistributables)
}

