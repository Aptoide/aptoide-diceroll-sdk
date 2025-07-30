import com.aptoide.diceroll.sdk.convention.extensions.projectImplementation

plugins {
  id("diceroll.android.feature.data")
}

android {
  namespace = "com.aptoide.diceroll.sdk.feature.stats.data"
}

dependencies {
  projectImplementation(":core:utils")
  projectImplementation(":core:db")
  implementation(libs.androidx.datastore.preferences)
}
