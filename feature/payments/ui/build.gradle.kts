import com.aptoide.diceroll.sdk.convention.extensions.projectImplementation

plugins {
  id("diceroll.android.feature.ui")
}

android {
  namespace = "com.aptoide.diceroll.sdk.feature.payments.ui"
}

dependencies {
  compileOnly(fileTree(mapOf("dir" to "libs", "include" to "*.aar")))
  projectImplementation(":feature:roll-game:data")
  projectImplementation(":feature:settings:data")
  projectImplementation(":payments:billing")
  projectImplementation(":payments:data")
  projectImplementation(":core:ui:design")
  projectImplementation(":core:ui:widgets")
  projectImplementation(":core:utils")
  projectImplementation(":core:navigation")
  implementation(libs.bundles.coil)
}
