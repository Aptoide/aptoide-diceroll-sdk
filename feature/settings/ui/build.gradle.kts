import com.aptoide.diceroll.sdk.convention.extensions.projectImplementation

plugins {
  alias(libs.plugins.compose.compiler)
  id("diceroll.android.feature.ui")
  id("diceroll.android.library.compose")
}

android {
  namespace = "com.aptoide.diceroll.sdk.feature.settings.ui"
}

dependencies {
  projectImplementation(":payments:billing")
  projectImplementation(":feature:settings:data")
  projectImplementation(":feature:stats:data")
  projectImplementation(":feature:stats:ui")
  projectImplementation(":core:ui:design")
  projectImplementation(":core:ui:widgets")
  projectImplementation(":core:utils")
  projectImplementation(":core:navigation")
}
