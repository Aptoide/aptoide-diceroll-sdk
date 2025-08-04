package com.aptoide.diceroll.sdk.convention.extensions

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Configure Compose-specific options
 */
internal fun Project.configureAndroidCompose(
  commonExtension: CommonExtension<*, *, *, *, *>,
) {
  commonExtension.apply {
    buildFeatures {
      compose = true
    }

    dependencies {
      implementation("androidx-compose-runtime")
      implementation("androidx-compose-foundation")
      implementation("androidx-compose-accompanist-systemuicontroller")
      implementation("androidx-compose-accompanist-navigation-material")
      implementation("androidx-compose-material3")
    }
  }
}
