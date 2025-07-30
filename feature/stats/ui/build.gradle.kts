import com.aptoide.diceroll.sdk.convention.extensions.projectImplementation

plugins {
  id("diceroll.android.feature.ui")
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
