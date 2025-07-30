plugins {
  id("diceroll.android.library")
}

android {
  namespace = "com.aptoide.diceroll.sdk.core.datastore"
}

dependencies {
  implementation(libs.androidx.datastore.preferences)
}
