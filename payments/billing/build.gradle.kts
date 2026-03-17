import com.aptoide.diceroll.sdk.convention.extensions.projectImplementation

plugins {
  id("diceroll.android.library")
}

android {
  namespace = "com.aptoide.diceroll.sdk.payments.billing"

  productFlavors {
    // The convention plugin creates "googlePlay" and "aptoide" flavors for all library modules.
    // Here we only customize the aptoide flavor to add the public key build config field.
    getByName("aptoide") {
      buildConfigField(
        "String",
        "APTOIDE_PUBLIC_KEY",
        project.property("APTOIDE_PUBLIC_KEY").toString()
      )
    }
  }

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
  packaging {
    jniLibs {
      useLegacyPackaging = true
    }
  }
}

dependencies {
  projectImplementation(":core:analytics")
  projectImplementation(":core:network")
  projectImplementation(":core:ui:notifications")
  projectImplementation(":core:utils")
  projectImplementation(":feature:roll-game:data")
  projectImplementation(":feature:settings:data")
  projectImplementation(":payments:data")
  "googlePlayImplementation"(libs.google.billing)
  "aptoideImplementation"(libs.aptoide.billing)
  implementation(libs.bundles.network)
}
