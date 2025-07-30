plugins {
  id("diceroll.android.library.compose")
}

android {
  namespace = "com.aptoide.diceroll.sdk.core.ui.notifications"
}

dependencies {
  implementation(libs.bundles.androidx.compose)
  implementation(libs.androidx.compose.material.iconsExtended)
}
