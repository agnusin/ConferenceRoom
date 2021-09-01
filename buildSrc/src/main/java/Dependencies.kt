import org.gradle.api.artifacts.dsl.RepositoryHandler

object Modules {
}

object Releases {
    private const val appVersionMajor = 1
    private const val appVersionMinor = 0
    private const val appVersionPatch = 0

    val versionCode = (System.getenv("CI_PIPELINE_ID")?.toInt() ?: 1)
    val versionName = "$appVersionMajor.$appVersionMinor.$appVersionPatch"
    val compileSdk = 29
    val minSdk = 21
    val targetSdk = 29
}

object Versions {
    val buildTools = "29.0.0"
    val kotlin = "1.3.71"
    val coroutines = "1.3.5"
    val gradlePlugin = "3.5.2"

    val appcompat = "1.1.0"
    val core = "1.1.0"
    val fragment = "1.2.4"
    val navigation = "2.2.2"
    val design = "1.1.0"
    val constraintlayout = "1.1.3"
    val lifecycle = "2.2.0"

    val koin = "2.1.5"

    val junit = "4.12"
    val espresso = "3.2.0"
    val testrunner = "1.2.0"
}

object ProjectDeps {
    val androidGradlePlugin = "com.android.tools.build:gradle:${Versions.gradlePlugin}"
    val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    val navigationArgs = "androidx.navigation:navigation-safe-args-gradle-plugin:${Versions.navigation}"
}

object Libraries {
    val kotlin = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"
    val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
    val koin = arrayOf(
        "org.koin:koin-androidx-scope:${Versions.koin}",
        "org.koin:koin-androidx-viewmodel:${Versions.koin}",
        "org.koin:koin-androidx-fragment:${Versions.koin}",
        "org.koin:koin-androidx-ext:${Versions.koin}"
    )
}

object KtxLibraries {
    val core = "androidx.core:core-ktx:${Versions.core}"
    val fragment = "androidx.fragment:fragment-ktx:${Versions.fragment}"
    val lifecycle = arrayOf(
        "androidx.lifecycle:lifecycle-livedata-core-ktx:${Versions.lifecycle}",
        "androidx.lifecycle:lifecycle-livedata-ktx:${Versions.lifecycle}",
        "androidx.lifecycle:lifecycle-reactivestreams-ktx:${Versions.lifecycle}",
        "androidx.lifecycle:lifecycle-runtime-ktx:${Versions.lifecycle}",
        "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifecycle}"
    )
    val navigation = arrayOf(
        "androidx.navigation:navigation-fragment-ktx:${Versions.navigation}",
        "androidx.navigation:navigation-ui-ktx:${Versions.navigation}"
    )
}

object CompatLibraries {
    val appcompat = "androidx.appcompat:appcompat:${Versions.appcompat}"
    val fragment = "androidx.fragment:fragment:${Versions.fragment}"
    val design = "com.google.android.material:material:${Versions.design}"
    val constraintlayout = "androidx.constraintlayout:constraintlayout:${Versions.constraintlayout}"
}

object TestLibraries {
    val junit = "junit:junit:${Versions.junit}"
    val testRunner = "androidx.test:runner:${Versions.testrunner}"
    val espresso = "androidx.test.espresso:espresso-core:${Versions.espresso}"
}

fun addRepositories(handler: RepositoryHandler) {
    handler.google()
    handler.jcenter()
}

