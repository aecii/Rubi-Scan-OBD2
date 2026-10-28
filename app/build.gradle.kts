/*
 * Copyright 2026 aecii_3d
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

base {
    archivesName.set("ObdLinkScanner")
}

android {
    namespace = "com.example.obdscanner"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.obdscanner"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // CameraX dependencies for Video Track Recorder HUD Overlay
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

tasks.register("publishDebugUpdate") {
    group = "distribution"
    description = "Upload the debug APK and version.json to the OBDII update server."
    doLast {
        val apk = layout.buildDirectory.file("outputs/apk/debug/ObdLinkScanner-debug.apk").get().asFile
        val versionFile = file("version.json")
        if (!apk.isFile) {
            throw GradleException("Debug APK was not built: ${apk.absolutePath}")
        }
        val existingNotes = if (versionFile.isFile) {
            @Suppress("UNCHECKED_CAST")
            val parsed = groovy.json.JsonSlurper().parse(versionFile) as Map<String, Any?>
            parsed["releaseNotes"]?.toString().orEmpty()
        } else {
            ""
        }
        val payload = mapOf(
            "versionCode" to (android.defaultConfig.versionCode ?: 1),
            "versionName" to (android.defaultConfig.versionName ?: "0"),
            "downloadUrl" to "https://aecii.kindfox.org/OBDII/RubiScan-OBDII.apk",
            "releaseNotes" to existingNotes
        )
        versionFile.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(payload)) + "\n")
        val remoteDir = "aecii@aecii.kindfox.org:/var/www/html/OBDII"
        fun upload(source: java.io.File, remoteName: String) {
            exec {
                commandLine(
                    "scp",
                    "-o", "BatchMode=yes",
                    "-o", "StrictHostKeyChecking=accept-new",
                    source.absolutePath,
                    "$remoteDir/$remoteName"
                )
            }
        }
        upload(apk, "RubiScan-OBDII.apk")
        upload(versionFile, "version.json")
    }
}

afterEvaluate {
    tasks.named("assembleDebug") {
        finalizedBy("publishDebugUpdate")
    }
}
