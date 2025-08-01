package com.aptoide.diceroll.sdk.convention.plugins

import com.aptoide.diceroll.sdk.convention.Config
import com.aptoide.diceroll.sdk.convention.extensions.implementation
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class JvmLibraryPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    with(target) {
      pluginManager.apply {
        apply("kotlin")
        apply("org.jetbrains.kotlin.jvm")
      }
      extensions.configure(JavaPluginExtension::class.java) {
        sourceCompatibility = Config.jvm.javaVersion
        targetCompatibility = Config.jvm.javaVersion
      }

      tasks.withType(KotlinCompile::class.java) {
        kotlinOptions {
          jvmTarget = Config.jvm.kotlinJvm
          freeCompilerArgs = freeCompilerArgs
        }
      }

      dependencies.apply {
        implementation("kotlin.stdlib")
      }
    }
  }
}
