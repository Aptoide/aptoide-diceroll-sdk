plugins {
  alias(libs.plugins.compose.compiler)
  id("diceroll.android.library.compose")
}

android {
  namespace = "com.aptoide.diceroll.sdk.core.utils"
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
    implementation(libs.androidx.security.crypto)
    implementation(libs.appsflyer.sdk)
}
