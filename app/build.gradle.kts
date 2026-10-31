import com.android.build.api.artifact.SingleArtifact
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "si.odisej"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "si.odisej"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}

// Z1: pade, če je v ZDRUŽENEM manifestu (po manifest merge vseh knjižnic) mrežno dovoljenje.
abstract class CheckNoInternetTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val forbidden = setOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
        )
        val androidNs = "http://schemas.android.com/apk/res/android"
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val doc = factory.newDocumentBuilder().parse(mergedManifest.get().asFile)

        val found = mutableListOf<String>()
        for (tag in listOf("uses-permission", "uses-permission-sdk-23")) {
            val nodes = doc.getElementsByTagName(tag)
            for (i in 0 until nodes.length) {
                val name = (nodes.item(i) as org.w3c.dom.Element).getAttributeNS(androidNs, "name")
                if (name in forbidden) found += name
            }
        }
        if (found.isNotEmpty()) {
            throw GradleException(
                "Prepovedano mrežno dovoljenje v združenem manifestu: $found " +
                    "(${mergedManifest.get().asFile}). Glej CONTEXT.md Z1."
            )
        }
        report.get().asFile.writeText("OK: brez ${forbidden.joinToString()}\n")
    }
}

androidComponents {
    onVariants { variant ->
        val cap = variant.name.replaceFirstChar { it.uppercase() }
        tasks.register<CheckNoInternetTask>("check${cap}NoInternet") {
            group = "verification"
            description = "Preveri, da združeni manifest (${variant.name}) nima mrežnih dovoljenj."
            mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
            report.set(layout.buildDirectory.file("reports/checkNoInternet/${variant.name}.txt"))
        }
    }
}

val checkNoInternet = tasks.register("checkNoInternet") {
    group = "verification"
    description = "Preveri vse variante: brez INTERNET in ACCESS_NETWORK_STATE."
    dependsOn(tasks.withType<CheckNoInternetTask>())
}

tasks.named("check") {
    dependsOn(checkNoInternet)
}
