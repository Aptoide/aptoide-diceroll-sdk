import com.aptoide.diceroll.sdk.convention.extensions.projectImplementation

plugins {
  alias(libs.plugins.compose.compiler)
  id("diceroll.android.library.compose")
}

android {
  namespace = "com.aptoide.diceroll.sdk.core.ui.design"
}

dependencies {
  implementation(libs.bundles.androidx.compose)
  implementation(libs.androidx.compose.material.iconsExtended)
  projectImplementation(":feature:roll-game:data")
}
