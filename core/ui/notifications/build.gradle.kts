plugins {
  alias(libs.plugins.compose.compiler)
  id("diceroll.android.library.compose")
}

android {
  namespace = "com.aptoide.diceroll.sdk.core.ui.notifications"
  packaging {
    jniLibs {
      useLegacyPackaging = true
    }
  }
}

dependencies {
  implementation(libs.bundles.androidx.compose)
  implementation(libs.androidx.compose.material.iconsExtended)
}
