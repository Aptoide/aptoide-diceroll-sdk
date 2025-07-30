import com.aptoide.diceroll.sdk.convention.extensions.projectImplementation

plugins {
  id("diceroll.android.library")
}

android {
  namespace = "com.aptoide.diceroll.sdk.payments.data"
}

dependencies {
  projectImplementation(":core:ui:notifications")
  projectImplementation(":core:network")
  projectImplementation(":feature:roll-game:data")
  projectImplementation(":core:utils")
  projectImplementation(":core:ui:design")
}
