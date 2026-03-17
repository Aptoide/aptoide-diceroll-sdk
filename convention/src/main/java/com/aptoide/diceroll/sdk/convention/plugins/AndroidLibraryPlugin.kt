package com.aptoide.diceroll.sdk.convention.plugins

import com.android.build.gradle.LibraryExtension
import com.aptoide.diceroll.sdk.convention.Config
import com.aptoide.diceroll.sdk.convention.extensions.configureAndroidAndKotlin
import com.aptoide.diceroll.sdk.convention.extensions.implementation
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    with(target) {
      with(pluginManager) {
        apply("com.android.library")
        apply("kotlin-android")
        apply<HiltPlugin>()
      }

      extensions.configure<LibraryExtension> {
        configureAndroidAndKotlin(this)
        defaultConfig.targetSdk = Config.android.targetSdk
        defaultConfig.missingDimensionStrategy(Config.distributionFlavorDimension, "googlePlay")

        flavorDimensions.add(Config.versionFlavorDimension)
      }

      dependencies {
        implementation("kotlin.stdlib")
      }
    }
  }
}
