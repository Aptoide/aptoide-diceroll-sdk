import com.aptoide.diceroll.sdk.convention.extensions.projectImplementation

plugins {
  id("diceroll.android.library.compose")
}

android {
  namespace = "com.aptoide.diceroll.sdk.core.navigation"
}

dependencies {
  projectImplementation(":core:utils")
  projectImplementation(":core:ui:design")
  implementation(libs.bundles.androidx.compose)
  implementation(libs.bundles.androidx.compose.accompanist)
}
