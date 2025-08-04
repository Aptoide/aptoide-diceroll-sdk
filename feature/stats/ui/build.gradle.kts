import com.aptoide.diceroll.sdk.convention.extensions.projectImplementation

plugins {
  alias(libs.plugins.compose.compiler)
  id("diceroll.android.feature.ui")
  id("diceroll.android.library.compose")
}

android {
  namespace = "com.aptoide.diceroll.sdk.feature.stats.ui"
}

dependencies {
  projectImplementation(":feature:roll-game:data")
  projectImplementation(":feature:settings:data")
  projectImplementation(":feature:stats:data")
  projectImplementation(":core:ui:design")
  projectImplementation(":core:ui:widgets")
  projectImplementation(":core:utils")
  projectImplementation(":core:navigation")
  implementation(libs.charts.tehras)
}
