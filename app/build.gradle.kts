import com.aptoide.diceroll.sdk.convention.extensions.projectImplementation

plugins {
  alias(libs.plugins.compose.compiler)
  id("diceroll.android.app")
}

android {
  namespace = "com.aptoide.diceroll.sdk"
  defaultConfig {
    applicationId = "com.aptoide.diceroll.sdk"
    versionCode = 1010
    versionName = "1.1.0"
    multiDexEnabled = true
  }
    buildTypes {
        debug {
            buildConfigField(
                "String",
                "APPSFLYER_API_KEY",
                project.property("APPSFLYER_API_KEY").toString()
            )

        }
        release {
            buildConfigField(
                "String",
                "APPSFLYER_API_KEY",
                project.property("APPSFLYER_API_KEY").toString()
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
  projectImplementation(":core:ui:design")
  projectImplementation(":core:ui:widgets")
  projectImplementation(":core:utils")
  projectImplementation(":core:permissions")
  projectImplementation(":core:navigation")
  projectImplementation(":feature:roll-game:data")
  projectImplementation(":feature:settings:data")
  projectImplementation(":feature:settings:ui")
  projectImplementation(":feature:stats:ui")
  projectImplementation(":feature:store:ui")
  projectImplementation(":feature:roll-game:ui")
  projectImplementation(":feature:payments:ui")
  projectImplementation(":payments:billing")
  projectImplementation(":payments:data")
  implementation(libs.androidx.splashscreen)
  implementation(libs.bundles.androidx.compose)
  implementation(libs.bundles.androidx.compose.accompanist)
  implementation(libs.appsflyer.sdk)
}
