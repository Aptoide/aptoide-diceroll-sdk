plugins {
  alias(libs.plugins.compose.compiler)
  id("diceroll.android.library.compose")
}

android {
  namespace = "com.aptoide.diceroll.sdk.core.analytics"
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
    implementation(libs.appsflyer.sdk)
}
