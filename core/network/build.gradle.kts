import com.aptoide.diceroll.sdk.convention.extensions.projectImplementation

plugins {
  id("diceroll.android.library")
}

android {
  namespace = "com.aptoide.diceroll.sdk.core.network"
}

dependencies {
  projectImplementation(":feature:settings:data")
  projectImplementation(":core:utils")
  implementation(libs.bundles.network)
}
