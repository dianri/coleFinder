import java.util.Properties
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

val secretsProps = Properties().apply {
    val defaultsFile = rootProject.file("secrets.defaults.properties")
    if (defaultsFile.exists()) load(defaultsFile.inputStream())
    val secretsFile = rootProject.file("secrets.properties")
    if (secretsFile.exists()) load(secretsFile.inputStream())
}

fun getSecret(key: String): String = secretsProps.getProperty(key, "")

val appVersionName = "1.1.2"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.secrets)
    id("jacoco")
}

android {
    namespace = "es.colefinder"
    compileSdk = 36

    defaultConfig {
        applicationId = "es.colefinder"
        minSdk = 30
        targetSdk = 36
        // Versionado semántico: MAJOR.MINOR.PATCH
        // versionCode: incrementar en +1 en cada release (nunca decrementar).
        // versionName: "MAJOR.MINOR.PATCH" — actualizar junto a versionCode
        //   MAJOR: cambio incompatible o rediseño mayor
        //   MINOR: nueva funcionalidad compatible
        //   PATCH: bugfix o mejora menor
        versionCode = 9
        versionName = "1.1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "env"
    productFlavors {
        create("pre") {
            dimension = "env"
            applicationIdSuffix = ".pre"
            resValue("string", "app_name", "ColeFinder PRE")
            buildConfigField("String", "SUPABASE_URL", "\"${getSecret("SUPABASE_URL_PRE")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${getSecret("SUPABASE_ANON_KEY_PRE")}\"")
            buildConfigField("String", "SUPABASE_SCHEMA", "\"staging\"")
        }
        create("prod") {
            dimension = "env"
            buildConfigField("String", "SUPABASE_URL", "\"${getSecret("SUPABASE_URL_PROD")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${getSecret("SUPABASE_ANON_KEY_PROD")}\"")
            buildConfigField("String", "SUPABASE_SCHEMA", "\"public\"")
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
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
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.all {
            it.extensions.configure(JacocoTaskExtension::class) {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }
    }
}

kotlin {
    jvmToolchain(17)
}

jacoco {
    toolVersion = "0.8.11"
}

private val jacocoExcludes = listOf(
    "**/R.class",
    "**/R\$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "android/**/*.*",
    "**/*Hilt*.*",
    "**/*_Factory*.*",
    "**/*_MembersInjector*.*",
    "**/*Module_*.*",
    "**/hilt_aggregated_deps/**",
    "**/dagger/**",
    "**/*ComposableSingletons*.*",
    "**/ui/theme/**",
    "**/*\$\$serializer*.*",
    // Compose UI — no testeables con JUnit (necesitan tests instrumentados)
    "**/ui/map/MapScreen*.*",
    "**/ui/map/components/**",
    "**/ui/map/*Screen*.*",
    "**/ui/map/*Content*.*",
    "**/ui/map/*Dialog*.*",
    "**/ui/map/*Card*.*",
    "**/ui/map/*Row*.*",
    "**/ui/map/*Item*.*",
    "**/ui/map/*Button*.*",
    "**/ui/update/**",
    // Hilt DI modules — no testeables unitariamente
    "**/di/**",
    // MainActivity
    "**/MainActivity*.*",
)

// Tarea reutilizable que genera el informe HTML + XML para un flavor+buildType
fun registerJacocoTask(
    flavorName: String,
    buildTypeName: String
) {
    val variantName = "$flavorName${buildTypeName.replaceFirstChar { it.uppercaseChar() }}"
    val testTaskName = "test${variantName.replaceFirstChar { it.uppercaseChar() }}UnitTest"
    val reportTaskName = "jacoco${variantName.replaceFirstChar { it.uppercaseChar() }}UnitTestReport"

    tasks.register<JacocoReport>(reportTaskName) {
        dependsOn(testTaskName)
        group = "Reporting"
        description = "Genera informe de cobertura Jacoco para $variantName"

        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(false)
        }

        val variantDir = variantName.replaceFirstChar { it.uppercaseChar() }
        val javaClasses = fileTree(
            layout.buildDirectory
                .dir("intermediates/javac/$variantName/compile${variantDir}JavaWithJavac/classes")
                .get()
                .asFile
        ) { exclude(jacocoExcludes) }

        val kotlinClasses = fileTree(
            layout.buildDirectory.dir("tmp/kotlin-classes/$variantName").get().asFile
        ) { exclude(jacocoExcludes) }

        classDirectories.setFrom(files(javaClasses, kotlinClasses))

        sourceDirectories.setFrom(
            files(
                "src/main/java",
                "src/main/kotlin"
            )
        )

        executionData.setFrom(
            fileTree(layout.buildDirectory.get().asFile) {
                include("outputs/unit_test_code_coverage/${variantName}UnitTest/test${variantDir}UnitTest.exec")
                include("jacoco/test${variantDir}UnitTest.exec")
            }
        )
    }
}

registerJacocoTask("pre", "debug")
registerJacocoTask("prod", "debug")

tasks.register<JacocoReport>("jacocoFullReport") {
    dependsOn("jacocoPreDebugUnitTestReport", "jacocoProdDebugUnitTestReport")
    group = "Reporting"
    description =
        "Informe combinando sesiones pre+prod sobre bytecode preDebug (evita duplicar clases por flavor)."

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val preDir = "preDebug"
    val preCap = "PreDebug"
    classDirectories.setFrom(
        files(
            fileTree(
                layout.buildDirectory
                    .dir("intermediates/javac/$preDir/compile${preCap}JavaWithJavac/classes")
                    .get()
                    .asFile
            ) { exclude(jacocoExcludes) },
            fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/$preDir").get().asFile) { exclude(jacocoExcludes) }
        )
    )

    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))

    executionData.setFrom(
        fileTree(layout.buildDirectory.get().asFile) {
            include("outputs/unit_test_code_coverage/preDebugUnitTest/test${preCap}UnitTest.exec")
            include("outputs/unit_test_code_coverage/prodDebugUnitTest/testProdDebugUnitTest.exec")
            include("jacoco/**/test${preCap}UnitTest.exec")
            include("jacoco/**/testProdDebugUnitTest.exec")
        }
    )
}

// Nombres de APK: colefinder-{pre|prod}-{versionName}-{buildType}.apk
// (AGP 8.13 no expone archivesName en productFlavors; se aplica vía applicationVariants.)
android.applicationVariants.configureEach {
    val v = this
    val base = when (v.flavorName) {
        "pre" -> "colefinder-pre-${v.versionName}"
        "prod" -> "colefinder-prod-${v.versionName}"
        else -> v.name
    }
    outputs.configureEach {
        val output = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
        output.outputFileName = "$base-${v.buildType.name}.apk"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Supabase
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.okhttp3)
    implementation(libs.ktor.client.logging)
    implementation(libs.kotlinx.serialization.json)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Play In-App Updates (flexible)
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)

    // Maps and Location
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// Secrets Gradle Plugin: lee secrets.properties (no versionado) y
// secrets.defaults.properties (versionado con placeholders) y los expone como BuildConfig fields.
secrets {
    propertiesFileName = "secrets.properties"
    defaultPropertiesFileName = "secrets.defaults.properties"
}