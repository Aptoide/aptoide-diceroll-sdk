import com.aptoide.diceroll.sdk.convention.extensions.projectImplementation

plugins {
  alias(libs.plugins.compose.compiler)
  id("diceroll.android.library.compose")
}

android {
  namespace = "com.aptoide.diceroll.sdk.core.ui.widgets"

  buildFeatures {
    buildConfig = true
  }

  defaultConfig {
    buildConfigField(
      "String",
      "SDK_BILLING_LIBRARY_VERSION",
      "\"${libs.android.aptoide.billing.get().version}\""
    )
  }
}

dependencies {
  projectImplementation(":core:ui:design")
  implementation(libs.bundles.androidx.compose)
  implementation(libs.lottie)
}
