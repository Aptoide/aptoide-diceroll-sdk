import com.aptoide.diceroll.sdk.convention.extensions.projectImplementation

plugins {
  id("diceroll.android.library")
  id("diceroll.room")
}

android {
  namespace = "com.aptoide.diceroll.sdk.core.db"
  packaging {
    jniLibs {
      useLegacyPackaging = true
    }
  }
}

dependencies {
  projectImplementation(":core:utils")
}
