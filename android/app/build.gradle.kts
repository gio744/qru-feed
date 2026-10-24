import java.security.MessageDigest
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}
android {
    namespace = "br.com.qru.transito"
    compileSdk = 35

    defaultConfig {
        applicationId = if (project.hasProperty("qruParallelTest")) "br.com.qru.testeapp" else "br.com.qru.transito"
        minSdk = 28
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.7-indice"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    signingConfigs {
        create("parallelTest") {
            storeFile = file("../test-signing/qru-debug.jks")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    buildTypes.getByName("debug") {
        if (project.hasProperty("qruParallelTest")) {
            signingConfig = signingConfigs.getByName("parallelTest")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.bouncycastle:bcprov-jdk18on:1.79")
    ksp("androidx.room:room-compiler:2.6.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

 
// Download only during the build. The installed app reads the bundled asset offline.
// Pin the original bytes: an upstream replacement must be reviewed explicitly.
val normAssets = layout.buildDirectory.dir("generated/normAssets")
val bundleNormPdf = tasks.register("bundleNormPdf") {
    val catalog=file("src/main/assets/norms/pdf-catalog.json")
    inputs.file(catalog)
    outputs.dir(normAssets)
    doLast {
        @Suppress("UNCHECKED_CAST")
        val documents=groovy.json.JsonSlurper().parse(catalog) as List<Map<String,Any>>
        documents.forEach { doc ->
            val name=doc["file"] as String
            require(name.matches(Regex("[A-Za-z0-9]+\\.pdf")))
            val destination=normAssets.get().file("norms/$name").asFile
            fun fingerprint(file:File):String = MessageDigest.getInstance("SHA-256")
                .digest(file.readBytes()).joinToString("") { "%02x".format(it) }
            val expected=doc["sha256"] as String
            if(!destination.exists() || fingerprint(destination)!=expected) {
                destination.parentFile.mkdirs()
                val temporary=File(destination.parentFile,"$name.download")
                try {
                    var lastFailure: Exception? = null
                    for (attempt in 1..3) {
                        try {
                            val connection=URI(doc["source"] as String).toURL().openConnection()
                            connection.connectTimeout=30000;connection.readTimeout=60000
                            connection.setRequestProperty("User-Agent","QRU-document-build/1.0")
                            val contentType=connection.contentType ?: "unknown"
                            val status=(connection as? java.net.HttpURLConnection)?.responseCode
                            connection.getInputStream().use { input ->
                                temporary.outputStream().use { output -> input.copyTo(output) }
                            }
                            val header=temporary.inputStream().use { it.readNBytes(5) }
                            if (!header.contentEquals("%PDF-".toByteArray(Charsets.US_ASCII))) {
                                throw IOException(
                                    "Official PDF response is not a PDF: $name; HTTP=${status ?: "unknown"}; " +
                                        "Content-Type=$contentType; bytes=${temporary.length()}; " +
                                        "source=${doc["source"]}. Keep pinned SHA-256 unchanged."
                                )
                            }
                            lastFailure=null
                            break
                        } catch (failure: java.io.IOException) {
                            lastFailure=failure
                            temporary.delete()
                            if(attempt<3) Thread.sleep(1500L * attempt)
                        }
                    }
                    if(lastFailure!=null) throw lastFailure
                    val actual=fingerprint(temporary)
                    check(actual==expected) {
                        "Official PDF integrity mismatch: $name; expected SHA-256=$expected; " +
                            "downloaded SHA-256=$actual; source=${doc["source"]}. " +
                            "Review the official publication before changing the pinned hash."
                    }
                    Files.move(temporary.toPath(),destination.toPath(),StandardCopyOption.REPLACE_EXISTING)
                } finally { temporary.delete() }
            }
        }
    }
}
android.sourceSets.getByName("main").assets.srcDir(normAssets)
tasks.named("preBuild") { dependsOn(bundleNormPdf) }
