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

        flavorDimensions.add(Config.distributionFlavorDimension)

        // Declare distribution flavors in every library module so that Gradle's
        // attribute-based variant resolution works across the full dependency graph.
        // Modules that don't need distribution-specific code use src/main for all
        // variants; only :payments:billing overrides these with flavor source sets.
        productFlavors.create("googlePlay") {
          dimension = Config.distributionFlavorDimension
        }
        productFlavors.create("aptoide") {
          dimension = Config.distributionFlavorDimension
        }
      }

      dependencies {
        implementation("kotlin.stdlib")
      }
    }
  }
}
