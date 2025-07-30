import com.aptoide.diceroll.sdk.convention.extensions.projectImplementation

plugins {
  id("diceroll.android.library")
}

android {
  namespace = "com.aptoide.diceroll.sdk.payments.billing"
  buildTypes {
    debug {
      buildConfigField(
        "String",
        "PUBLIC_KEY",
        project.property("DICEROLL_SDK_PUBLIC_KEY_DEV").toString()
      )

    }
    release {
      buildConfigField(
        "String",
        "PUBLIC_KEY",
        project.property("DICEROLL_SDK_PUBLIC_KEY").toString()
      )
    }
  }
  buildFeatures {
    buildConfig = true
  }
}

dependencies {
  projectImplementation(":core:network")
  projectImplementation(":core:ui:notifications")
  projectImplementation(":core:utils")
  projectImplementation(":feature:roll-game:data")
  projectImplementation(":feature:settings:data")
  projectImplementation(":payments:data")
  implementation(libs.android.aptoide.billing)
  implementation(libs.google.billing)
  implementation(libs.bundles.network)
}
