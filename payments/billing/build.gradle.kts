import com.aptoide.diceroll.sdk.convention.extensions.projectImplementation

plugins {
  id("diceroll.android.library")
}

android {
  namespace = "com.aptoide.diceroll.sdk.payments.billing"
  val aptoideAppPublicKey = providers.gradleProperty("APTOIDE_APP_PUBLIC_KEY")
    .orElse(providers.gradleProperty("APTOIDE_APP_PUBLIC_KEY_DEV"))
    .orElse("")
    .get()

  flavorDimensions.clear()
  flavorDimensions += "distribution"
  productFlavors {
    create("googlePlay") {
      dimension = "distribution"
      buildConfigField("String", "APTOIDE_APP_PUBLIC_KEY", "\"\"")
    }
    create("aptoide") {
      dimension = "distribution"
      buildConfigField("String", "APTOIDE_APP_PUBLIC_KEY", "\"$aptoideAppPublicKey\"")
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
  add("googlePlayImplementation", libs.google.billing)
  add("aptoideImplementation", libs.aptoide.billing.sdk)
  implementation(libs.bundles.network)
}
