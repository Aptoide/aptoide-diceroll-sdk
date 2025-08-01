import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `kotlin-dsl`
}

group = "com.aptoide.diceroll.sdk.convention"

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
}

dependencies {
  implementation(libs.gradlePlugin.android)
  implementation(libs.gradlePlugin.kotlin)
  implementation(libs.gradlePlugin.hilt)
  implementation(libs.gradlePlugin.ksp)
}

gradlePlugin {
  plugins {
    register("AndroidApp") {
      id = "diceroll.android.app"
      implementationClass = "com.aptoide.diceroll.sdk.convention.plugins.AndroidAppPlugin"
    }
    register("AndroidFeatureData") {
      id = "diceroll.android.feature.data"
      implementationClass = "com.aptoide.diceroll.sdk.convention.plugins.AndroidFeatureDataPlugin"
    }
    register("AndroidFeature") {
      id = "diceroll.android.feature.ui"
      implementationClass = "com.aptoide.diceroll.sdk.convention.plugins.AndroidFeatureUiPlugin"
    }
    register("AndroidLibrary") {
      id = "diceroll.android.library"
      implementationClass = "com.aptoide.diceroll.sdk.convention.plugins.AndroidLibraryPlugin"
    }
    register("AndroidLibraryCompose") {
      id = "diceroll.android.library.compose"
      implementationClass = "com.aptoide.diceroll.sdk.convention.plugins.AndroidLibraryComposePlugin"
    }
    register("JvmLibrary") {
      id = "diceroll.jvm.library"
      implementationClass = "com.aptoide.diceroll.sdk.convention.plugins.JvmLibraryPlugin"
    }
    register("Room") {
      id = "diceroll.room"
      implementationClass = "com.aptoide.diceroll.sdk.convention.plugins.RoomPlugin"
    }
    register("Hilt") {
      id = "diceroll.hilt"
      implementationClass = "com.aptoide.diceroll.sdk.convention.plugins.HiltPlugin"
    }
  }
}
