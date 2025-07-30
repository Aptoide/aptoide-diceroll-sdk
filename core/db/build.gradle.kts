import com.aptoide.diceroll.sdk.convention.extensions.projectImplementation

plugins {
  id("diceroll.android.library")
  id("diceroll.room")
}

android {
  namespace = "com.aptoide.diceroll.sdk.core.db"
}

dependencies {
  projectImplementation(":core:utils")
}
